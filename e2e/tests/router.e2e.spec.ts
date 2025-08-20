import { test, expect, request } from '@playwright/test';

test.describe('Fast Router Service E2E', () => {
  const activemqUrl = 'http://localhost:8161';
  const kafkaUiUrl = 'http://localhost:8090';
  const healthUrl = 'http://localhost:8080/health';

  const validXml = `<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.13">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>MSG-IT</MsgId>
      <CreDtTm>2024-01-15T10:30:00Z</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <CtrlSum>1000.00</CtrlSum>
      <TtlIntrBkSttlmAmt Ccy="SGD">1000.00</TtlIntrBkSttlmAmt>
      <IntrBkSttlmDt>2024-01-15</IntrBkSttlmDt>
      <SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId><EndToEndId>E2E-IT-001</EndToEndId></PmtId>
      <IntrBkSttlmAmt Ccy="SGD">1000.00</IntrBkSttlmAmt>
      <ChrgBr>SHAR</ChrgBr>
      <Dbtr><Nm>John Doe</Nm></Dbtr>
      <DbtrAcct><Id><Othr><Id>123456789</Id></Othr></Id></DbtrAcct>
      <DbtrAgt><FinInstnId><BICFI>AAAAUS33</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>BBBBUS33</BICFI></FinInstnId></CdtrAgt>
      <Cdtr><Nm>Jane Smith</Nm></Cdtr>
      <CdtrAcct><Id><Othr><Id>987654321</Id></Othr></Id></CdtrAcct>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>`;

  test('health and message flow', async ({ page }) => {
    // Health endpoint is up
    const h = await fetch(healthUrl);
    expect(h.ok).toBeTruthy();

    // Post XML to ActiveMQ REST
    const mqReq = await request.newContext({ httpCredentials: { username: 'admin', password: 'admin' } });
    const resp = await mqReq.post(`${activemqUrl}/api/message/payment.inbound?type=queue`, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      form: { body: validXml }
    });
    expect(resp.ok()).toBeTruthy();

    // Open Kafka UI and wait for topic page to render
    await page.goto(kafkaUiUrl);
    await page.getByText('local').click();
    await page.getByText('payment-messages').click();
    // Expect some JSON content appears eventually
    await expect(page.getByText('messageType').first()).toBeVisible({ timeout: 15000 });
  });
});


