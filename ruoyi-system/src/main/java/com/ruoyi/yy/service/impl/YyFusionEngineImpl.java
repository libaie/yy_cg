package com.ruoyi.yy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.yy.constant.FusionConfidence;
import com.ruoyi.yy.domain.*;
import com.ruoyi.yy.mapper.YyDrugAliasMapper;
import com.ruoyi.yy.mapper.YyFusionReviewMapper;
import com.ruoyi.yy.service.IYyDrugMasterService;
import com.ruoyi.yy.service.IYyMatchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多信号融合引擎
 *
 * 流程：
 * 1. 检查yy_drug_alias缓存 -> 命中直接返回
 * 2. 按优先级执行IYyMatchStrategy策略链
 * 3. 根据置信度决定：自动接受 / 待复核 / 进入审核队列
 */
@Service
public class YyFusionEngineImpl {

    private static final Logger log = LoggerFactory.getLogger(YyFusionEngineImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<IYyMatchStrategy> strategies;
    private final IYyDrugMasterService drugMasterService;
    private final YyDrugAliasMapper aliasMapper;
    private final YyFusionReviewMapper reviewMapper;

    @Autowired
    public YyFusionEngineImpl(List<IYyMatchStrategy> strategies,
                        IYyDrugMasterService drugMasterService,
                        YyDrugAliasMapper aliasMapper,
                        YyFusionReviewMapper reviewMapper) {
        this.strategies = strategies.stream()
            .sorted(Comparator.comparingInt(IYyMatchStrategy::getPriority).reversed())
            .collect(Collectors.toList());
        this.drugMasterService = drugMasterService;
        this.aliasMapper = aliasMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 对一个商品快照执行融合匹配
     */
    public YyFusionResult fuse(YyProductSnapshot snapshot) {
        String platformCode = snapshot.getSourcePlatform();
        String skuId = snapshot.getSkuId();

        // Step 1: 检查alias缓存
        YyDrugAlias cached = aliasMapper.selectYyDrugAliasByPlatformSku(platformCode, skuId);
        if (cached != null) {
            YyDrugMaster drug = drugMasterService.selectById(cached.getDrugId());
            if (drug != null) {
                aliasMapper.updateLastVerifiedAt(cached.getId(), new Date());
                log.debug("Alias cache hit: {} -> drug_id={}", skuId, cached.getDrugId());
                return YyFusionResult.matched(
                    drug.getId(), drug.getDrugCode(), cached.getConfidence(),
                    cached.getMatchMethod(), "Alias cache hit", false
                );
            }
            log.warn("Dangling alias found for sku={}, deleting", skuId);
            aliasMapper.deleteYyDrugAliasById(cached.getId());
        }

        // Step 2: 获取候选集（供策略使用）
        List<YyDrugMaster> candidates = getCandidates(snapshot);

        // Step 3: 按优先级执行策略链
        YyMatchResult bestResult = null;
        for (IYyMatchStrategy strategy : strategies) {
            log.debug("Trying strategy {} for sku={}", strategy.getName(), skuId);
            YyMatchResult result = strategy.match(snapshot, candidates);
            if (result.isMatched()) {
                bestResult = result;
                break;
            }
        }

        // Step 4: 处理结果
        if (bestResult != null && bestResult.isMatched()) {
            boolean needsReview = FusionConfidence.needsReview(bestResult.getConfidence());

            saveAlias(snapshot, bestResult);

            log.info("Fusion matched: sku={} -> drug_id={} via {} conf={}",
                skuId, bestResult.getDrugId(), bestResult.getMatchMethod(), bestResult.getConfidence());

            return YyFusionResult.matched(
                bestResult.getDrugId(), bestResult.getDrugCode(),
                bestResult.getConfidence(), bestResult.getMatchMethod().getCode(),
                bestResult.getReason(), needsReview
            );
        }

        // 未匹配 -> 进入审核队列
        log.info("Fusion no match: sku={}, sending to review queue", skuId);
        saveToReviewQueue(snapshot, candidates);

        List<YyCandidateDrug> candidateDrugs = candidates.stream()
            .map(d -> new YyCandidateDrug(d.getId(), null))
            .collect(Collectors.toList());
        return YyFusionResult.noMatch(candidateDrugs);
    }

    private List<YyDrugMaster> getCandidates(YyProductSnapshot snapshot) {
        String commonName = snapshot.getCommonName();
        if (commonName != null && !commonName.trim().isEmpty()) {
            return drugMasterService.selectCandidates(commonName.trim(), null);
        }
        return new ArrayList<>();
    }

    private void saveAlias(YyProductSnapshot snapshot, YyMatchResult result) {
        YyDrugAlias existing = aliasMapper.selectYyDrugAliasByPlatformSku(
            snapshot.getSourcePlatform(), snapshot.getSkuId());

        if (existing != null) {
            existing.setDrugId(result.getDrugId());
            existing.setConfidence(result.getConfidence());
            existing.setMatchMethod(result.getMatchMethod().getCode());
            existing.setLastVerifiedAt(new Date());
            aliasMapper.updateYyDrugAlias(existing);
            log.info("Alias updated: sku={} -> drug_id={} (was {})",
                snapshot.getSkuId(), result.getDrugId(), existing.getDrugId());
        } else {
            YyDrugAlias alias = new YyDrugAlias();
            alias.setDrugId(result.getDrugId());
            alias.setPlatformCode(snapshot.getSourcePlatform());
            alias.setPlatformProductName(snapshot.getCommonName());
            alias.setPlatformManufacturer(snapshot.getManufacturer());
            alias.setPlatformSpecification(snapshot.getSpecification());
            alias.setPlatformSkuId(snapshot.getSkuId());
            alias.setConfidence(result.getConfidence());
            alias.setMatchMethod(result.getMatchMethod().getCode());
            alias.setLastVerifiedAt(new Date());
            aliasMapper.insertYyDrugAlias(alias);
        }
    }

    private void saveToReviewQueue(YyProductSnapshot snapshot, List<YyDrugMaster> candidates) {
        try {
            List<Long> candidateIds = candidates.stream()
                .map(YyDrugMaster::getId).collect(Collectors.toList());

            YyFusionReview review = new YyFusionReview();
            review.setSnapshotId(snapshot.getId());
            review.setCandidateDrugIds(JSON.writeValueAsString(candidateIds));
            review.setMatchScores("[]");
            review.setStatus("pending");
            reviewMapper.insertYyFusionReview(review);
        } catch (Exception e) {
            log.error("Failed to save review queue entry for sku={}", snapshot.getSkuId(), e);
            throw new RuntimeException("Review queue persistence failed for sku=" + snapshot.getSkuId(), e);
        }
    }
}
