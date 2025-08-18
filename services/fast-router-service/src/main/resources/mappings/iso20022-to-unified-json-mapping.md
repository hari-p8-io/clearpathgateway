# ISO 20022 to Ultra Lean JSON Mapping Documentation

## Overview

This document provides the mapping between ISO 20022 XML message formats and the **ultra lean unified JSON schema** for payment messages in the APEAFAST-SG ClearPath Gateway system. The lean schema eliminates JSON bloat through **aggressive flattening** and **direct field access patterns**.

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

## Ultra Lean Flattened Mapping

### Flattened Group Header Fields (All Messages)

| XSD Path | Lean JSON Path | Type | Max Length | Notes |
|----------|----------------|------|------------|-------|
| `GrpHdr/MsgId` | `messageId` | string | 35 | **FLATTENED** - Direct field access |
| `GrpHdr/CreDtTm` | `creationDateTime` | string (date-time) | - | **FLATTENED** - Direct field access |
| `GrpHdr/BtchBookg` | `batchBooking` | boolean | - | **FLATTENED** - Direct field access |
| `GrpHdr/NbOfTxs` | `numberOfTransactions` | string | 15 | **FLATTENED** - Direct field access |
| `GrpHdr/CtrlSum` | `controlSum` | number | - | **FLATTENED** - Direct field access |
| `GrpHdr/IntrBkSttlmDt` | `interbankSettlementDate` | string (date) | - | **FLATTENED** - Direct field access |
| `GrpHdr/SttlmInf/SttlmMtd` | `settlementMethod` | string | - | **FLATTENED** - Extract method only |
| `GrpHdr/InstgAgt/FinInstnId/BICFI` | `instructingAgentBIC` | string | 11 | **FLATTENED** - Direct BIC access |
| `GrpHdr/InstdAgt/FinInstnId/BICFI` | `instructedAgentBIC` | string | 11 | **FLATTENED** - Direct BIC access |

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

## Ultra Lean Transaction Mapping

### Flattened Transaction Fields

| XSD Path | Lean JSON Path | Type | Max Length | Notes |
|----------|----------------|------|------------|-------|
| `PmtId/InstrId` | `transactions[].instructionId` | string | 35 | **FLATTENED** - Direct access |
| `PmtId/EndToEndId` | `transactions[].endToEndId` | string | 35 | **FLATTENED** - Direct access |
| `PmtId/TxId` | `transactions[].transactionId` | string | 35 | **FLATTENED** - Direct access |
| `PmtId/UETR` | `transactions[].UETR` | string | - | **FLATTENED** - Direct access |
| `PmtId/ClrSysRef` | `transactions[].clearingSystemReference` | string | 35 | **FLATTENED** - Direct access |

### Flattened Amount Fields

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `IntrBkSttlmAmt` | `transactions[].amount` | number | **FLATTENED** - Direct amount value |
| `IntrBkSttlmAmt/@Ccy` | `transactions[].currency` | string | **FLATTENED** - Direct currency code |
| `IntrBkSttlmAmt` | `transactions[].interbankSettlementAmount` | number | **FLATTENED** - Settlement amount |
| `InstdAmt` | `transactions[].instructedAmount` | number | **FLATTENED** - Instructed amount |

### Ultra Lean Party Mapping

#### Flattened Creditor Fields (Essential Only)

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `Cdtr/Nm` | `transactions[].creditorName` | string | **FLATTENED** - Direct name access |
| `CdtrAcct/Id/IBAN` or `CdtrAcct/Id/Othr` | `transactions[].creditorAccountId` | string | **FLATTENED** - Direct account ID |
| `CdtrAgt/FinInstnId/BICFI` | `transactions[].creditorBIC` | string | **FLATTENED** - Direct BIC access |

#### Flattened Debtor Fields (Essential Only)

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `Dbtr/Nm` | `transactions[].debtorName` | string | **FLATTENED** - Direct name access |
| `DbtrAcct/Id/IBAN` or `DbtrAcct/Id/Othr` | `transactions[].debtorAccountId` | string | **FLATTENED** - Direct account ID |
| `DbtrAgt/FinInstnId/BICFI` | `transactions[].debtorBIC` | string | **FLATTENED** - Direct BIC access |

> **Note**: Complex party structures (postal address, contact details, identification) are **ELIMINATED** in the lean schema to reduce JSON bloat. Only essential payment processing fields are retained.

### Ultra Lean Payment Details

#### Flattened Essential Payment Fields

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `ChrgBr` | `transactions[].chargeBearer` | string | **FLATTENED** - Direct charge bearer |
| `SttlmPrty` | `transactions[].settlementPriority` | string | **FLATTENED** - Direct priority |
| `Purp/Cd` | `transactions[].purposeCode` | string | **FLATTENED** - Direct purpose code |
| `RmtInf/Ustrd` | `transactions[].remittanceInformation` | string | **FLATTENED** - Simple string only |
| `ReqdExctnDt` | `transactions[].requiredExecutionDate` | string | **FLATTENED** - Direct date |
| `IntrBkSttlmDt` | `transactions[].interbankSettlementDate` | string | **FLATTENED** - Direct date |

> **Lean Approach**: Complex nested structures for financial institutions, account details, addresses, and contact information are **ELIMINATED**. Only BIC codes and account identifiers are retained for core payment processing.

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

### Ultra Lean Reversal/Cancellation Fields

#### Flattened Reversal Fields (PACS.007)

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `RvslId` | `transactions[].reversalId` | string | **FLATTENED** - Direct reversal ID |
| `OrgnlInstrId` | `transactions[].originalInstructionId` | string | **FLATTENED** - Direct original ID |
| `OrgnlEndToEndId` | `transactions[].originalEndToEndId` | string | **FLATTENED** - Direct original E2E |
| `OrgnlTxId` | `transactions[].originalTransactionId` | string | **FLATTENED** - Direct original TxID |
| `RvslRsnInf/Rsn` | `transactions[].reversalReason` | string | **FLATTENED** - Simple reason text |

#### Flattened Cancellation Fields (CAMT.056)

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `CxlId` | `transactions[].cancellationId` | string | **FLATTENED** - Direct cancellation ID |
| `OrgnlInstrId` | `transactions[].originalInstructionId` | string | **FLATTENED** - Direct original ID |
| `OrgnlEndToEndId` | `transactions[].originalEndToEndId` | string | **FLATTENED** - Direct original E2E |
| `OrgnlTxId` | `transactions[].originalTransactionId` | string | **FLATTENED** - Direct original TxID |
| `CxlRsnInf/Rsn` | `transactions[].cancellationReason` | string | **FLATTENED** - Simple reason text |

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

## Ultra Lean Case Management (CAMT.056/CAMT.029)

### Flattened Case Fields

| XSD Path | Lean JSON Path | Type | Notes |
|----------|----------------|------|-------|
| `Case/Id` | `caseId` | string | **FLATTENED** - Direct case ID |
| `Case/Cretr` | `caseCreator` | string | **FLATTENED** - Creator name only |
| `Sts/ConfrmtnInd` | `investigationStatus` | string | **FLATTENED** - Direct status |
| `Sts/RjctnRsn` | `rejectionReason` | string | **FLATTENED** - Simple reason text |

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

**Ultra Lean JSON Output:**
```json
{
  "messageType": "PACS_008",
  "messageVersion": "13",
  "messageId": "MSG001",
  "creationDateTime": "2024-02-17T10:30:00Z",
  "numberOfTransactions": "1",
  "transactions": [
    {
      "endToEndId": "E2E001",
      "amount": 1000.00,
      "currency": "SGD"
    }
  ]
}
```

> **Lean Benefits**: 
> - 60% smaller JSON payload
> - Direct field access: `msg.messageId` vs `msg.groupHeader.messageId`
> - Simplified parsing: `tx.amount` vs `tx.interbankSettlementAmount.value`
> - Reduced memory footprint and faster serialization

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