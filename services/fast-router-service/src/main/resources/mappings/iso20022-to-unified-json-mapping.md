# ISO 20022 to Unified JSON Mapping Documentation

## Overview

This document provides the mapping between ISO 20022 XML message formats and the unified JSON schema for payment messages in the APEAFAST-SG ClearPath Gateway system.

**Supported Message Types:**
- `pacs.008.001.13` - FI To FI Customer Credit Transfer (PACS_008)
- `pacs.003.001.11` - FI To FI Customer Direct Debit (PACS_003)
- `pacs.007.001.13` - FI To FI Payment Reversal (PACS_007)
- `camt.056.001.11` - FI To FI Payment Cancellation Request (CAMT_056)
- `pacs.002.001.15` - FI To FI Payment Status Report (PACS_002)
- `camt.029.001.13` - Resolution of Investigation (CAMT_029)

## Message Type Mapping

| ISO 20022 Message | Unified JSON messageType | Description | Service Flow |
|-------------------|---------------------------|-------------|---------------|
| `pacs.008.001.13` | `PACS_008` | Customer Credit Transfer | Inward/Outward |
| `pacs.003.001.11` | `PACS_003` | Customer Direct Debit | Inward/Outward |
| `pacs.007.001.13` | `PACS_007` | Payment Reversal | Inward/Outward |
| `camt.056.001.11` | `CAMT_056` | Payment Cancellation Request | Inward/Outward |
| `pacs.002.001.15` | `PACS_002` | Payment Status Report | Response (Router) |
| `camt.029.001.13` | `CAMT_029` | Resolution of Investigation | Response (Router) |

## Root Document Structure Mapping

### PACS.008 - FI To FI Customer Credit Transfer

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/FIToFICstmrCdtTrf` | Root object | object | Main document container |
| `Document/FIToFICstmrCdtTrf/GrpHdr` | `groupHeader` | object | Group header information |
| `Document/FIToFICstmrCdtTrf/CdtTrfTxInf` | `transactionInformation[]` | array | Credit transfer transaction information |
| `Document/FIToFICstmrCdtTrf/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

### PACS.003 - FI To FI Customer Direct Debit

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/FIToFICstmrDrctDbt` | Root object | object | Main document container |
| `Document/FIToFICstmrDrctDbt/GrpHdr` | `groupHeader` | object | Group header information |
| `Document/FIToFICstmrDrctDbt/DrctDbtTxInf` | `transactionInformation[]` | array | Direct debit transaction information |
| `Document/FIToFICstmrDrctDbt/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

### PACS.007 - FI To FI Payment Reversal

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/FIToFIPmtRvsl` | Root object | object | Main document container |
| `Document/FIToFIPmtRvsl/GrpHdr` | `groupHeader` | object | Group header information |
| `Document/FIToFIPmtRvsl/OrgnlGrpInf` | `originalGroupInformation` | object | Original group information |
| `Document/FIToFIPmtRvsl/TxInf` | `transactionInformation[]` | array | Transaction information |
| `Document/FIToFIPmtRvsl/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

### CAMT.056 - FI To FI Payment Cancellation Request

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/FIToFIPmtCxlReq` | Root object | object | Main document container |
| `Document/FIToFIPmtCxlReq/Assgnmt` | `groupHeader` | object | Case assignment (mapped to group header) |
| `Document/FIToFIPmtCxlReq/Case` | `caseInformation` | object | Case information |
| `Document/FIToFIPmtCxlReq/CtrlData` | `controlData` | object | Control data |
| `Document/FIToFIPmtCxlReq/Undrlyg` | `transactionInformation[]` | array | Underlying transaction information |
| `Document/FIToFIPmtCxlReq/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

## Group Header Mapping

### Common Group Header Fields (All Messages)

| XSD Path | JSON Path | Type | Max Length | Notes |
|----------|-----------|------|------------|-------|
| `GrpHdr/MsgId` | `groupHeader.messageId` | string | 35 | Message identification |
| `GrpHdr/CreDtTm` | `groupHeader.creationDateTime` | string (date-time) | - | Creation date and time |
| `GrpHdr/Authstn` | `groupHeader.authorisation[]` | array | - | Authorisation information |
| `GrpHdr/BtchBookg` | `groupHeader.batchBooking` | boolean | - | Batch booking indicator |
| `GrpHdr/NbOfTxs` | `groupHeader.numberOfTransactions` | string | 15 | Number of transactions |
| `GrpHdr/CtrlSum` | `groupHeader.controlSum` | number | - | Control sum |
| `GrpHdr/TtlIntrBkSttlmAmt` | `groupHeader.totalInterbankSettlementAmount` | object | - | Total interbank settlement amount |
| `GrpHdr/IntrBkSttlmDt` | `groupHeader.interbankSettlementDate` | string (date) | - | Interbank settlement date |
| `GrpHdr/SttlmInf` | `groupHeader.settlementInformation` | object | - | Settlement information |
| `GrpHdr/PmtTpInf` | `groupHeader.paymentTypeInformation` | object | - | Payment type information |
| `GrpHdr/InstgAgt` | `groupHeader.instructingAgent` | object | - | Instructing agent |
| `GrpHdr/InstdAgt` | `groupHeader.instructedAgent` | object | - | Instructed agent |

### PACS.007 Specific Group Header Fields

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `GrpHdr/GrpRvsl` | `groupHeader.groupReversal` | boolean | Group reversal indicator |
| `GrpHdr/TtlRvsdIntrBkSttlmAmt` | `groupHeader.totalReversedInterbankSettlementAmount` | object | Total reversed amount |

### CAMT.056 Case Assignment Mapping (to Group Header)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Assgnmt/Id` | `groupHeader.messageId` | string | Assignment ID mapped to message ID |
| `Assgnmt/CreDtTm` | `groupHeader.creationDateTime` | string (date-time) | Creation date and time |
| `Assgnmt/Assgnr` | `groupHeader.instructingAgent` | object | Assignor mapped to instructing agent |
| `Assgnmt/Assgne` | `groupHeader.instructedAgent` | object | Assignee mapped to instructed agent |

## Transaction Information Mapping

### Payment Identification Mapping

| XSD Path | JSON Path | Type | Max Length | Notes |
|----------|-----------|------|------------|-------|
| `PmtId/InstrId` | `transactionInformation[].paymentIdentification.instructionId` | string | 35 | Instruction ID |
| `PmtId/EndToEndId` | `transactionInformation[].paymentIdentification.endToEndId` | string | 35 | End-to-end ID |
| `PmtId/TxId` | `transactionInformation[].paymentIdentification.transactionId` | string | 35 | Transaction ID |
| `PmtId/UETR` | `transactionInformation[].paymentIdentification.UETR` | string | - | Universal End-to-End Transaction Reference |
| `PmtId/ClrSysRef` | `transactionInformation[].paymentIdentification.clearingSystemReference` | string | 35 | Clearing system reference |

### Amount and Currency Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `IntrBkSttlmAmt` | `transactionInformation[].interbankSettlementAmount.value` | number | Amount value |
| `IntrBkSttlmAmt/@Ccy` | `transactionInformation[].interbankSettlementAmount.currency` | string | Currency code |
| `InstdAmt` | `transactionInformation[].instructedAmount.value` | number | Instructed amount value |
| `InstdAmt/@Ccy` | `transactionInformation[].instructedAmount.currency` | string | Currency code |

### Party Information Mapping

#### Creditor Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Cdtr/Nm` | `transactionInformation[].creditor.name` | string | Creditor name |
| `Cdtr/PstlAdr` | `transactionInformation[].creditor.postalAddress` | object | Postal address |
| `Cdtr/Id` | `transactionInformation[].creditor.identification` | object | Party identification |
| `Cdtr/CtryOfRes` | `transactionInformation[].creditor.countryOfResidence` | string | Country of residence |
| `Cdtr/CtctDtls` | `transactionInformation[].creditor.contactDetails` | object | Contact details |

#### Debtor Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Dbtr/Nm` | `transactionInformation[].debtor.name` | string | Debtor name |
| `Dbtr/PstlAdr` | `transactionInformation[].debtor.postalAddress` | object | Postal address |
| `Dbtr/Id` | `transactionInformation[].debtor.identification` | object | Party identification |
| `Dbtr/CtryOfRes` | `transactionInformation[].debtor.countryOfResidence` | string | Country of residence |
| `Dbtr/CtctDtls` | `transactionInformation[].debtor.contactDetails` | object | Contact details |

### Financial Institution Mapping

#### Creditor Agent Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CdtrAgt/FinInstnId/BICFI` | `transactionInformation[].creditorAgent.financialInstitutionIdentification.BICFI` | string | BIC code |
| `CdtrAgt/FinInstnId/ClrSysMmbId` | `transactionInformation[].creditorAgent.financialInstitutionIdentification.clearingSystemMemberId` | object | Clearing system member ID |
| `CdtrAgt/FinInstnId/LEI` | `transactionInformation[].creditorAgent.financialInstitutionIdentification.LEI` | string | Legal Entity Identifier |
| `CdtrAgt/FinInstnId/Nm` | `transactionInformation[].creditorAgent.financialInstitutionIdentification.name` | string | Institution name |
| `CdtrAgt/FinInstnId/PstlAdr` | `transactionInformation[].creditorAgent.financialInstitutionIdentification.postalAddress` | object | Postal address |
| `CdtrAgt/BrnchId` | `transactionInformation[].creditorAgent.branchIdentification` | object | Branch identification |

#### Debtor Agent Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `DbtrAgt/FinInstnId/BICFI` | `transactionInformation[].debtorAgent.financialInstitutionIdentification.BICFI` | string | BIC code |
| `DbtrAgt/FinInstnId/ClrSysMmbId` | `transactionInformation[].debtorAgent.financialInstitutionIdentification.clearingSystemMemberId` | object | Clearing system member ID |
| `DbtrAgt/FinInstnId/LEI` | `transactionInformation[].debtorAgent.financialInstitutionIdentification.LEI` | string | Legal Entity Identifier |
| `DbtrAgt/FinInstnId/Nm` | `transactionInformation[].debtorAgent.financialInstitutionIdentification.name` | string | Institution name |
| `DbtrAgt/FinInstnId/PstlAdr` | `transactionInformation[].debtorAgent.financialInstitutionIdentification.postalAddress` | object | Postal address |
| `DbtrAgt/BrnchId` | `transactionInformation[].debtorAgent.branchIdentification` | object | Branch identification |

### Account Information Mapping

#### Creditor Account Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CdtrAcct/Id/IBAN` | `transactionInformation[].creditorAccount.identification.IBAN` | string | IBAN |
| `CdtrAcct/Id/Othr` | `transactionInformation[].creditorAccount.identification.other` | object | Other account identification |
| `CdtrAcct/Tp` | `transactionInformation[].creditorAccount.type` | object | Account type |
| `CdtrAcct/Ccy` | `transactionInformation[].creditorAccount.currency` | string | Account currency |
| `CdtrAcct/Nm` | `transactionInformation[].creditorAccount.name` | string | Account name |
| `CdtrAcct/Prxy` | `transactionInformation[].creditorAccount.proxy` | object | Proxy account identification |

#### Debtor Account Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `DbtrAcct/Id/IBAN` | `transactionInformation[].debtorAccount.identification.IBAN` | string | IBAN |
| `DbtrAcct/Id/Othr` | `transactionInformation[].debtorAccount.identification.other` | object | Other account identification |
| `DbtrAcct/Tp` | `transactionInformation[].debtorAccount.type` | object | Account type |
| `DbtrAcct/Ccy` | `transactionInformation[].debtorAccount.currency` | string | Account currency |
| `DbtrAcct/Nm` | `transactionInformation[].debtorAccount.name` | string | Account name |
| `DbtrAcct/Prxy` | `transactionInformation[].debtorAccount.proxy` | object | Proxy account identification |

### Payment Type Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `PmtTpInf/InstrPrty` | `transactionInformation[].paymentTypeInformation.instructionPriority` | string | Instruction priority |
| `PmtTpInf/ClrChanl` | `transactionInformation[].paymentTypeInformation.clearingChannel` | string | Clearing channel |
| `PmtTpInf/SvcLvl` | `transactionInformation[].paymentTypeInformation.serviceLevel[]` | array | Service level |
| `PmtTpInf/LclInstrm` | `transactionInformation[].paymentTypeInformation.localInstrument` | object | Local instrument |
| `PmtTpInf/SeqTp` | `transactionInformation[].paymentTypeInformation.sequenceType` | string | Sequence type |
| `PmtTpInf/CtgyPurp` | `transactionInformation[].paymentTypeInformation.categoryPurpose` | object | Category purpose |

### Direct Debit Transaction Mapping (PACS.003 Specific)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `DrctDbtTx/MndtRltdInf` | `transactionInformation[].directDebitTransaction.mandateRelatedInformation` | object | Mandate related information |
| `DrctDbtTx/CdtrSchmeId` | `transactionInformation[].directDebitTransaction.creditorSchemeIdentification` | object | Creditor scheme identification |
| `DrctDbtTx/PreNtfctnId` | `transactionInformation[].directDebitTransaction.preNotificationId` | string | Pre-notification ID |
| `DrctDbtTx/PreNtfctnDt` | `transactionInformation[].directDebitTransaction.preNotificationDate` | string | Pre-notification date |

### Mandate Related Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `MndtRltdInf/MndtId` | `mandateRelatedInformation.mandateId` | string | Mandate ID |
| `MndtRltdInf/DtOfSgntr` | `mandateRelatedInformation.dateOfSignature` | string | Date of signature |
| `MndtRltdInf/AmdmntInd` | `mandateRelatedInformation.amendmentIndicator` | boolean | Amendment indicator |
| `MndtRltdInf/AmdmntInfDtls` | `mandateRelatedInformation.amendmentInformationDetails` | object | Amendment information details |
| `MndtRltdInf/ElctrncSgntr` | `mandateRelatedInformation.electronicSignature` | string | Electronic signature |
| `MndtRltdInf/FrstColltnDt` | `mandateRelatedInformation.firstCollectionDate` | string | First collection date |
| `MndtRltdInf/FnlColltnDt` | `mandateRelatedInformation.finalCollectionDate` | string | Final collection date |
| `MndtRltdInf/Frqcy` | `mandateRelatedInformation.frequency` | object | Frequency |
| `MndtRltdInf/Rsn` | `mandateRelatedInformation.reason` | object | Reason |
| `MndtRltdInf/TrckgDays` | `mandateRelatedInformation.trackingDays` | string | Tracking days |

### Reversal Specific Mapping (PACS.007)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `RvslId` | `transactionInformation[].reversalId` | string | Reversal ID |
| `OrgnlInstrId` | `transactionInformation[].originalInstructionId` | string | Original instruction ID |
| `OrgnlEndToEndId` | `transactionInformation[].originalEndToEndId` | string | Original end-to-end ID |
| `OrgnlTxId` | `transactionInformation[].originalTransactionId` | string | Original transaction ID |
| `OrgnlUETR` | `transactionInformation[].originalUETR` | string | Original UETR |
| `OrgnlClrSysRef` | `transactionInformation[].originalClearingSystemReference` | string | Original clearing system reference |
| `OrgnlIntrBkSttlmAmt` | `transactionInformation[].originalInterbankSettlementAmount` | object | Original interbank settlement amount |
| `RvsdIntrBkSttlmAmt` | `transactionInformation[].reversedInterbankSettlementAmount` | object | Reversed interbank settlement amount |
| `RvsdInstdAmt` | `transactionInformation[].reversedInstructedAmount` | object | Reversed instructed amount |
| `CompstnAmt` | `transactionInformation[].compensationAmount` | object | Compensation amount |
| `RvslRsnInf` | `transactionInformation[].reversalReasonInformation[]` | array | Reversal reason information |

### Cancellation Specific Mapping (CAMT.056)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CxlId` | `transactionInformation[].cancellationId` | string | Cancellation ID |
| `OrgnlInstrId` | `transactionInformation[].originalInstructionId` | string | Original instruction ID |
| `OrgnlEndToEndId` | `transactionInformation[].originalEndToEndId` | string | Original end-to-end ID |
| `OrgnlTxId` | `transactionInformation[].originalTransactionId` | string | Original transaction ID |
| `OrgnlUETR` | `transactionInformation[].originalUETR` | string | Original UETR |
| `OrgnlClrSysRef` | `transactionInformation[].originalClearingSystemReference` | string | Original clearing system reference |
| `OrgnlIntrBkSttlmAmt` | `transactionInformation[].originalInterbankSettlementAmount` | object | Original interbank settlement amount |
| `CxlRsnInf` | `transactionInformation[].cancellationReasonInformation[]` | array | Cancellation reason information |
| `OrgnlTxRef` | `transactionInformation[].originalTransactionReference` | object | Original transaction reference |

### Remittance Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `RmtInf/Ustrd` | `transactionInformation[].remittanceInformation.unstructured[]` | array | Unstructured remittance information |
| `RmtInf/Strd` | `transactionInformation[].remittanceInformation.structured[]` | array | Structured remittance information |

### Structured Remittance Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Strd/RfrdDocInf` | `structured[].referredDocumentInformation[]` | array | Referred document information |
| `Strd/RfrdDocAmt` | `structured[].referredDocumentAmount` | object | Referred document amount |
| `Strd/CdtrRefInf` | `structured[].creditorReferenceInformation` | object | Creditor reference information |
| `Strd/Invcr` | `structured[].invoicer` | object | Invoicer |
| `Strd/Invcee` | `structured[].invoicee` | object | Invoicee |
| `Strd/TaxRmt` | `structured[].taxRemittance` | object | Tax remittance |
| `Strd/GrnshmtRmt` | `structured[].garnishmentRemittance` | object | Garnishment remittance |
| `Strd/AddtlRmtInf` | `structured[].additionalRemittanceInformation[]` | array | Additional remittance information |

### Charges Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `ChrgsInf/Amt` | `transactionInformation[].chargesInformation[].amount` | object | Charge amount |
| `ChrgsInf/Agt` | `transactionInformation[].chargesInformation[].agent` | object | Charge agent |
| `ChrgsInf/Tp` | `transactionInformation[].chargesInformation[].type` | object | Charge type |

### Regulatory Reporting Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `RgltryRptg/DbtCdtRptgInd` | `transactionInformation[].regulatoryReporting[].debitCreditReportingIndicator` | string | Debit credit reporting indicator |
| `RgltryRptg/Authrty` | `transactionInformation[].regulatoryReporting[].authority` | object | Regulatory authority |
| `RgltryRptg/Dtls` | `transactionInformation[].regulatoryReporting[].details[]` | array | Regulatory reporting details |

### Settlement Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `SttlmInf/SttlmMtd` | `settlementInformation.settlementMethod` | string | Settlement method |
| `SttlmInf/SttlmAcct` | `settlementInformation.settlementAccount` | object | Settlement account |
| `SttlmInf/ClrSys` | `settlementInformation.clearingSystem` | object | Clearing system |
| `SttlmInf/InstgRmbrsmntAgt` | `settlementInformation.instructingReimbursementAgent` | object | Instructing reimbursement agent |
| `SttlmInf/InstgRmbrsmntAgtAcct` | `settlementInformation.instructingReimbursementAgentAccount` | object | Instructing reimbursement agent account |
| `SttlmInf/InstdRmbrsmntAgt` | `settlementInformation.instructedReimbursementAgent` | object | Instructed reimbursement agent |
| `SttlmInf/InstdRmbrsmntAgtAcct` | `settlementInformation.instructedReimbursementAgentAccount` | object | Instructed reimbursement agent account |
| `SttlmInf/ThrdRmbrsmntAgt` | `settlementInformation.thirdReimbursementAgent` | object | Third reimbursement agent |
| `SttlmInf/ThrdRmbrsmntAgtAcct` | `settlementInformation.thirdReimbursementAgentAccount` | object | Third reimbursement agent account |

### Postal Address Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `PstlAdr/AdrTp` | `postalAddress.addressType` | object | Address type |
| `PstlAdr/CareOf` | `postalAddress.careOf` | string | Care of |
| `PstlAdr/Dept` | `postalAddress.department` | string | Department |
| `PstlAdr/SubDept` | `postalAddress.subDepartment` | string | Sub-department |
| `PstlAdr/StrtNm` | `postalAddress.streetName` | string | Street name |
| `PstlAdr/BldgNb` | `postalAddress.buildingNumber` | string | Building number |
| `PstlAdr/BldgNm` | `postalAddress.buildingName` | string | Building name |
| `PstlAdr/Flr` | `postalAddress.floor` | string | Floor |
| `PstlAdr/UnitNb` | `postalAddress.unitNumber` | string | Unit number |
| `PstlAdr/PstBx` | `postalAddress.postBox` | string | Post box |
| `PstlAdr/Room` | `postalAddress.room` | string | Room |
| `PstlAdr/PstCd` | `postalAddress.postCode` | string | Post code |
| `PstlAdr/TwnNm` | `postalAddress.townName` | string | Town name |
| `PstlAdr/TwnLctnNm` | `postalAddress.townLocationName` | string | Town location name |
| `PstlAdr/DstrctNm` | `postalAddress.districtName` | string | District name |
| `PstlAdr/CtrySubDvsn` | `postalAddress.countrySubDivision` | string | Country subdivision |
| `PstlAdr/Ctry` | `postalAddress.country` | string | Country |
| `PstlAdr/AdrLine` | `postalAddress.addressLine[]` | array | Address lines |

### Contact Details Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CtctDtls/NmPrfx` | `contactDetails.namePrefix` | string | Name prefix |
| `CtctDtls/Nm` | `contactDetails.name` | string | Name |
| `CtctDtls/PhneNb` | `contactDetails.phoneNumber` | string | Phone number |
| `CtctDtls/MobNb` | `contactDetails.mobileNumber` | string | Mobile number |
| `CtctDtls/FaxNb` | `contactDetails.faxNumber` | string | Fax number |
| `CtctDtls/EmailAdr` | `contactDetails.emailAddress` | string | Email address |
| `CtctDtls/Othr` | `contactDetails.other[]` | array | Other contact information |

### Party Identification Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Id/OrgId` | `identification.organisationIdentification` | object | Organisation identification |
| `Id/PrvtId` | `identification.privateIdentification` | object | Private identification |

### Organisation Identification Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `OrgId/AnyBIC` | `organisationIdentification.anyBIC` | string | Any BIC |
| `OrgId/LEI` | `organisationIdentification.LEI` | string | Legal Entity Identifier |
| `OrgId/Othr` | `organisationIdentification.other[]` | array | Other organisation identification |

### Private Identification Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `PrvtId/DtAndPlcOfBirth` | `privateIdentification.dateAndPlaceOfBirth` | object | Date and place of birth |
| `PrvtId/Othr` | `privateIdentification.other[]` | array | Other person identification |

### Purpose Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Purp/Cd` | `purpose.code` | string | Purpose code |
| `Purp/Prtry` | `purpose.proprietary` | string | Proprietary purpose |

### Supplementary Data Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `SplmtryData/PlcAndNm` | `supplementaryData[].placeAndName` | string | Place and name |
| `SplmtryData/Envlp` | `supplementaryData[].envelope` | object | Envelope (any additional data) |

## Case Information Mapping (CAMT.056 Specific)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Case/Id` | `caseInformation.id` | string | Case ID |
| `Case/Cretr` | `caseInformation.creator` | object | Case creator |
| `Case/ReopCaseIndctn` | `caseInformation.reopenCaseIndication` | boolean | Reopen case indication |

## Control Data Mapping (CAMT.056 Specific)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CtrlData/NbOfTxs` | `controlData.numberOfTransactions` | string | Number of transactions |
| `CtrlData/CtrlSum` | `controlData.controlSum` | number | Control sum |

## Original Group Information Mapping (PACS.007 and CAMT.056)

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `OrgnlGrpInf/OrgnlMsgId` | `originalGroupInformation.originalMessageId` | string | Original message ID |
| `OrgnlGrpInf/OrgnlMsgNmId` | `originalGroupInformation.originalMessageNameId` | string | Original message name ID |
| `OrgnlGrpInf/OrgnlCreDtTm` | `originalGroupInformation.originalCreationDateTime` | string | Original creation date time |
| `OrgnlGrpInf/RvslRsnInf` | `originalGroupInformation.reversalReasonInformation[]` | array | Reversal reason information (PACS.007) |

## Reason Information Mapping

### Cancellation Reason Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `CxlRsnInf/Orgtr` | `cancellationReasonInformation[].originator` | object | Originator |
| `CxlRsnInf/Rsn` | `cancellationReasonInformation[].reason` | object | Cancellation reason |
| `CxlRsnInf/AddtlInf` | `cancellationReasonInformation[].additionalInformation[]` | array | Additional information |

### Reversal Reason Information Mapping

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `RvslRsnInf/Orgtr` | `reversalReasonInformation[].originator` | object | Originator |
| `RvslRsnInf/Rsn` | `reversalReasonInformation[].reason` | object | Reversal reason |
| `RvslRsnInf/AddtlInf` | `reversalReasonInformation[].additionalInformation[]` | array | Additional information |

## Code/Choice Mapping Pattern

Many fields in ISO 20022 follow a pattern of having either a code or proprietary value. These are mapped as follows:

| XSD Pattern | JSON Pattern | Notes |
|-------------|-------------|-------|
| `Element/Cd` | `element.code` | Standard code value |
| `Element/Prtry` | `element.proprietary` | Proprietary/custom value |

Examples:
- `ServiceLevel/Cd` → `serviceLevel.code`
- `ServiceLevel/Prtry` → `serviceLevel.proprietary`
- `LocalInstrument/Cd` → `localInstrument.code`
- `LocalInstrument/Prtry` → `localInstrument.proprietary`

## Enumeration Mappings

### Message Type Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| FIToFICstmrCdtTrf | PACS_008 | Customer Credit Transfer |
| FIToFICstmrDrctDbt | PACS_003 | Customer Direct Debit |
| FIToFIPmtRvsl | PACS_007 | Payment Reversal |
| FIToFIPmtCxlReq | CAMT_056 | Payment Cancellation Request |

### Priority Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| HIGH | HIGH | High priority |
| NORM | NORM | Normal priority |
| URGT | URGT | Urgent priority |

### Clearing Channel Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| RTGS | RTGS | Real Time Gross Settlement |
| RTNS | RTNS | Real Time Net Settlement |
| MPNS | MPNS | Mass Payment Net Settlement |
| BOOK | BOOK | Book Transfer |

### Settlement Method Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| INDA | INDA | Instructed Agent |
| INGA | INGA | Instructing Agent |
| COVE | COVE | Cover Method |
| CLRG | CLRG | Clearing |

### Charge Bearer Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| DEBT | DEBT | Debtor |
| CRED | CRED | Creditor |
| SHAR | SHAR | Shared |
| SLEV | SLEV | Service Level |

### Sequence Type Enumerations

| XSD Value | JSON Value | Description |
|-----------|------------|-------------|
| FRST | FRST | First |
| RCUR | RCUR | Recurring |
| FNAL | FNAL | Final |
| OOFF | OOFF | One Off |
| RPRE | RPRE | Represented |

## Data Type Conversions

### Date and Time Conversions

| XSD Type | JSON Type | Format | Example |
|----------|-----------|--------|---------|
| ISODate | string | date | "2024-02-17" |
| ISODateTime | string | date-time | "2024-02-17T10:30:00Z" |
| ISOYear | integer | - | 2024 |

### Amount Conversions

ISO 20022 amounts are represented as decimal values with currency attributes, while JSON represents them as objects:

**XSD:**
```xml
<IntrBkSttlmAmt Ccy="SGD">1000.00</IntrBkSttlmAmt>
```

**JSON:**
```json
{
  "interbankSettlementAmount": {
    "value": 1000.00,
    "currency": "SGD"
  }
}
```

### Boolean Conversions

| XSD Type | JSON Type | Notes |
|----------|-----------|-------|
| xs:boolean | boolean | Direct mapping |
| TrueFalseIndicator | boolean | Direct mapping |
| YesNoIndicator | boolean | Direct mapping |

### Text Length Constraints

| XSD Type | Max Length | JSON Validation |
|----------|------------|-----------------|
| Max35Text | 35 | maxLength: 35 |
| Max70Text | 70 | maxLength: 70 |
| Max140Text | 140 | maxLength: 140 |
| Max256Text | 256 | maxLength: 256 |
| Max350Text | 350 | maxLength: 350 |
| Max2048Text | 2048 | maxLength: 2048 |

## Pattern Validations

### Identifier Patterns

| XSD Type | Pattern | JSON Pattern |
|----------|---------|-------------|
| BICFIDec2014Identifier | `[A-Z0-9]{4,4}[A-Z]{2,2}[A-Z0-9]{2,2}([A-Z0-9]{3,3}){0,1}` | `^[A-Z0-9]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$` |
| LEIIdentifier | `[A-Z0-9]{18,18}[0-9]{2,2}` | `^[A-Z0-9]{18}[0-9]{2}$` |
| IBAN2007Identifier | `[A-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}` | `^[A-Z]{2}[0-9]{2}[a-zA-Z0-9]{1,30}$` |
| UUIDv4Identifier | `[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}` | `^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$` |
| PhoneNumber | `\+[0-9]{1,3}-[0-9()+\-]{1,30}` | `^\\+[0-9]{1,3}-[0-9()+\\-]{1,30}$` |
| CountryCode | `[A-Z]{2,2}` | `^[A-Z]{2}$` |
| ActiveCurrencyCode | `[A-Z]{3,3}` | `^[A-Z]{3}$` |

## Usage Examples

### PACS.008 Credit Transfer Example

**XSD Input:**
```xml
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.13">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>MSG001</MsgId>
      <CreDtTm>2024-02-17T10:30:00Z</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <EndToEndId>E2E001</EndToEndId>
      </PmtId>
      <IntrBkSttlmAmt Ccy="SGD">1000.00</IntrBkSttlmAmt>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>
```

**JSON Output:**
```json
{
  "messageType": "PACS_008",
  "messageVersion": "13",
  "groupHeader": {
    "messageId": "MSG001",
    "creationDateTime": "2024-02-17T10:30:00Z",
    "numberOfTransactions": "1"
  },
  "transactionInformation": [
    {
      "paymentIdentification": {
        "endToEndId": "E2E001"
      },
      "interbankSettlementAmount": {
        "value": 1000.00,
        "currency": "SGD"
      }
    }
  ]
}
```

### PACS.002 - FI To FI Payment Status Report

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/FIToFIPmtStsRpt` | Root object | object | Main document container |
| `Document/FIToFIPmtStsRpt/GrpHdr` | `groupHeader` | object | Group header information |
| `Document/FIToFIPmtStsRpt/OrgnlGrpInfAndSts` | `originalGroupInformation[]` | array | Original group information and status |
| `Document/FIToFIPmtStsRpt/TxInfAndSts` | `transactionInformationAndStatus[]` | array | Transaction information and status |
| `Document/FIToFIPmtStsRpt/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

### CAMT.029 - Resolution of Investigation

| XSD Path | JSON Path | Type | Notes |
|----------|-----------|------|-------|
| `Document/RsltnOfInvstgtn` | Root object | object | Main document container |
| `Document/RsltnOfInvstgtn/Assgnmt` | `caseAssignment` | object | Case assignment information |
| `Document/RsltnOfInvstgtn/RslvdCase` | `resolutionCase` | object | Resolved case information |
| `Document/RsltnOfInvstgtn/Sts` | `investigationStatus` | object | Investigation status |
| `Document/RsltnOfInvstgtn/CxlDtls` | `cancellationDetails[]` | array | Cancellation details |
| `Document/RsltnOfInvstgtn/ModDtls` | `modificationDetails` | object | Modification details |
| `Document/RsltnOfInvstgtn/SplmtryData` | `supplementaryData[]` | array | Supplementary data |

## Error Handling and Validation

### Required Field Validation

The JSON schema enforces required fields based on message type:

- **PACS_008**: Requires `groupHeader`, `transactionInformation` with mandatory fields for credit transfer
- **PACS_003**: Requires `groupHeader`, `transactionInformation` with mandatory fields for direct debit
- **PACS_007**: Requires `groupHeader`, may require `originalGroupInformation` for reversals
- **CAMT_056**: Requires `groupHeader`, `transactionInformation`, may require `caseInformation` and `controlData`
- **PACS_002**: Requires `groupHeader`, may contain `originalGroupInformation` and `transactionInformationAndStatus`
- **CAMT_029**: Requires `caseAssignment`, `investigationStatus`, may contain `cancellationDetails` and `modificationDetails`

### Data Type Validation

All JSON fields are validated according to their data types, patterns, and length constraints as defined in the unified schema.

### Cross-Field Validation

Some validations require checking relationships between fields:

1. **Currency Consistency**: All amounts in a transaction should typically use the same currency
2. **Date Sequence**: Settlement dates should be logical relative to creation dates
3. **Reference Consistency**: Original references in reversal/cancellation messages should reference valid original transactions

## Conclusion

This mapping document provides comprehensive guidance for converting ISO 20022 XML messages to the unified JSON format used across the APEAFAST-SG ClearPath Gateway microservices. The unified schema ensures consistency while maintaining the semantic meaning and regulatory compliance requirements of the original ISO 20022 standards.

For implementation details and code examples, refer to the transformation utilities in the `fast-router-service` module.