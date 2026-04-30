package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 药品主数据 yy_drug_master
 */
public class YyDrugMaster extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String drugCode;
    private String commonName;
    private String genericName;
    private String barcode;
    private String approvalNumber;
    private String manufacturer;
    private String specification;
    private String dosageForm;
    private String categoryL1;
    private String categoryL2;
    private Integer isPrescription;
    private String medicareType;
    private Integer status;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getApprovalNumber() { return approvalNumber; }
    public void setApprovalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
    public String getCategoryL1() { return categoryL1; }
    public void setCategoryL1(String categoryL1) { this.categoryL1 = categoryL1; }
    public String getCategoryL2() { return categoryL2; }
    public void setCategoryL2(String categoryL2) { this.categoryL2 = categoryL2; }
    public Integer getIsPrescription() { return isPrescription; }
    public void setIsPrescription(Integer isPrescription) { this.isPrescription = isPrescription; }
    public String getMedicareType() { return medicareType; }
    public void setMedicareType(String medicareType) { this.medicareType = medicareType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
