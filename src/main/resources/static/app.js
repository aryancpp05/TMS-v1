const { useEffect, useMemo, useState } = React;

const api = {
  get: (path) => fetch(path).then(async (res) => {
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  }),
  post: (path, payload) => fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(async (res) => {
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  }),
  put: (path, payload) => fetch(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(async (res) => {
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  }),
  patch: (path, payload) => fetch(path, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(async (res) => {
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  })
};

function App() {
  const [tab, setTab] = useState('dashboard');
  const [summary, setSummary] = useState({ totalTransactions: 0, totalAlerts: 0, activeAlerts: 0, openAlerts: 0 });
  const [transactions, setTransactions] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [rules, setRules] = useState([]);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [error, setError] = useState('');
  const [simCount, setSimCount] = useState(10);
  const [operatorId, setOperatorId] = useState('op-001');
  const [requesterId, setRequesterId] = useState('customer-001');

  const [txForm, setTxForm] = useState({
    reference: '',
    accountId: 'ACC-001',
    payeeId: 'PAYEE-001',
    amount: 100,
    currency: 'USD',
    timestamp: new Date().toISOString().slice(0, 19) + 'Z',
    description: 'Manual test transaction'
  });

  const [txFilter, setTxFilter] = useState({ search: '', accountId: '', status: '' });
  const [alertFilter, setAlertFilter] = useState({ status: '', severity: '', activeOnly: true });

  const loadAll = async () => {
    try {
      setError('');
      const [summaryData, txData, alertData, ruleData] = await Promise.all([
        api.get('/api/dashboard/summary'),
        api.get('/api/transactions'),
        api.get('/api/alerts?activeOnly=true'),
        api.get('/api/rules')
      ]);
      setSummary(summaryData);
      setTransactions(txData);
      setAlerts(alertData);
      setRules(ruleData);
      if (selectedAlert) {
        const refreshed = await api.get(`/api/alerts/${selectedAlert.id}`);
        setSelectedAlert(refreshed);
      }
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const filteredTransactions = useMemo(() => {
    return transactions.filter(t => {
      const text = txFilter.search.toLowerCase();
      const textMatches = !text || t.reference.toLowerCase().includes(text) || t.description.toLowerCase().includes(text);
      const accountMatches = !txFilter.accountId || t.accountId === txFilter.accountId;
      const statusMatches = !txFilter.status || t.status === txFilter.status;
      return textMatches && accountMatches && statusMatches;
    });
  }, [transactions, txFilter]);

  const reviewTransaction = async (transactionId, approved) => {
    try {
      setError('');
      const note = approved ? 'Approved after rule violation review' : 'Rejected after rule violation review';
      await api.patch(`/api/transactions/${transactionId}/${approved ? 'approve' : 'reject'}`, {
        operatorId,
        note
      });
      await loadAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const requestRollback = async (transactionId) => {
    try {
      setError('');
      await api.patch(`/api/transactions/${transactionId}/rollback/request`, {
        reasonCode: 'CUSTOMER_REQUEST',
        reasonDetail: 'Rollback requested from dashboard workflow',
        requestedBy: requesterId,
        supportingReference: `CASE-${transactionId}`
      });
      await loadAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const reviewRollback = async (transactionId, approved) => {
    try {
      setError('');
      await api.patch(`/api/transactions/${transactionId}/rollback/${approved ? 'approve' : 'reject'}`, {
        operatorId,
        note: approved ? 'Rollback approved by operator' : 'Rollback rejected by operator'
      });
      await loadAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const loadAlerts = async () => {
    try {
      setError('');
      const params = new URLSearchParams();
      if (alertFilter.status) params.set('status', alertFilter.status);
      if (alertFilter.severity) params.set('severity', alertFilter.severity);
      params.set('activeOnly', alertFilter.activeOnly ? 'true' : 'false');
      const data = await api.get('/api/alerts?' + params.toString());
      setAlerts(data);
    } catch (e) {
      setError(e.message);
    }
  };

  const submitTransaction = async (event) => {
    event.preventDefault();
    try {
      setError('');
      await api.post('/api/transactions', {
        ...txForm,
        amount: Number(txForm.amount),
        timestamp: txForm.timestamp
      });
      setTxForm({
        ...txForm,
        reference: '',
        amount: 100,
        timestamp: new Date().toISOString().slice(0, 19) + 'Z'
      });
      await loadAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const generateSampleData = async () => {
    try {
      setError('');
      await api.post('/api/simulator/generate', { count: Number(simCount) });
      await loadAll();
      await loadAlerts();
    } catch (e) {
      setError(e.message);
    }
  };

  const openAlert = async (id) => {
    try {
      setError('');
      const data = await api.get(`/api/alerts/${id}`);
      setSelectedAlert(data);
      setTab('alerts');
    } catch (e) {
      setError(e.message);
    }
  };

  const updateAlertStatus = async (status) => {
    if (!selectedAlert) return;
    try {
      setError('');
      await api.patch(`/api/alerts/${selectedAlert.id}/status`, {
        status,
        note: `Updated from UI to ${status}`
      });
      const refreshed = await api.get(`/api/alerts/${selectedAlert.id}`);
      setSelectedAlert(refreshed);
      await loadAll();
      await loadAlerts();
    } catch (e) {
      setError(e.message);
    }
  };

  const updateRule = async (rule) => {
    try {
      setError('');
      await api.put(`/api/rules/${rule.id}`, {
        severity: rule.severity,
        active: rule.active,
        amountThreshold: rule.amountThreshold,
        transactionCountThreshold: rule.transactionCountThreshold,
        timeWindowMinutes: rule.timeWindowMinutes
      });
      const freshRules = await api.get('/api/rules');
      setRules(freshRules);
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="app">
      <div className="header">
        <h1>Transaction Monitoring Dashboard - V1</h1>
        <div className="small">Basic starter version with rules, alerts, and lifecycle actions.</div>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="tabs">
        <button className={tab === 'dashboard' ? 'active-tab' : ''} onClick={() => setTab('dashboard')}>Dashboard</button>
        <button className={tab === 'transactions' ? 'active-tab' : ''} onClick={() => setTab('transactions')}>Transactions</button>
        <button className={tab === 'alerts' ? 'active-tab' : ''} onClick={() => setTab('alerts')}>Alerts</button>
        <button className={tab === 'rules' ? 'active-tab' : ''} onClick={() => setTab('rules')}>Rules</button>
        <button onClick={loadAll}>Refresh</button>
      </div>

      {tab === 'dashboard' && (
        <div className="panel">
          <h3>Summary</h3>
          <div className="grid">
            <div className="card"><strong>{summary.totalTransactions}</strong><div>Total Transactions</div></div>
            <div className="card"><strong>{summary.totalAlerts}</strong><div>Total Alerts</div></div>
            <div className="card"><strong>{summary.activeAlerts}</strong><div>Active Alerts</div></div>
            <div className="card"><strong>{summary.openAlerts}</strong><div>Open Alerts</div></div>
          </div>
          <div style={{ marginTop: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
            <input type="number" min="1" max="200" value={simCount} onChange={e => setSimCount(e.target.value)} style={{ width: 120 }} />
            <button className="primary" onClick={generateSampleData}>Generate Sample Transactions</button>
          </div>
          <p className="small">Use the Transactions tab to generate test data and trigger alerts quickly.</p>
        </div>
      )}

      {tab === 'transactions' && (
        <>
          <div className="panel">
            <h3>Create Transaction</h3>
            <form onSubmit={submitTransaction}>
              <div className="filters">
                <input placeholder="Reference" value={txForm.reference} onChange={e => setTxForm({ ...txForm, reference: e.target.value })} required />
                <input placeholder="Account" value={txForm.accountId} onChange={e => setTxForm({ ...txForm, accountId: e.target.value })} required />
                <input placeholder="Payee" value={txForm.payeeId} onChange={e => setTxForm({ ...txForm, payeeId: e.target.value })} required />
                <input type="number" step="0.01" placeholder="Amount" value={txForm.amount} onChange={e => setTxForm({ ...txForm, amount: e.target.value })} required />
                <input placeholder="Currency" value={txForm.currency} onChange={e => setTxForm({ ...txForm, currency: e.target.value })} required />
                <input type="datetime-local" value={txForm.timestamp.replace('Z', '')} onChange={e => setTxForm({ ...txForm, timestamp: e.target.value + 'Z' })} required />
              </div>
              <textarea placeholder="Description" value={txForm.description} onChange={e => setTxForm({ ...txForm, description: e.target.value })} required />
              <div style={{ marginTop: 10 }}><button className="primary" type="submit">Submit Transaction</button></div>
            </form>
          </div>

          <div className="panel">
            <h3>Transactions</h3>
            <div className="filters">
              <input placeholder="Search by reference/description" value={txFilter.search} onChange={e => setTxFilter({ ...txFilter, search: e.target.value })} />
              <input placeholder="Filter account" value={txFilter.accountId} onChange={e => setTxFilter({ ...txFilter, accountId: e.target.value })} />
              <select value={txFilter.status} onChange={e => setTxFilter({ ...txFilter, status: e.target.value })}>
                <option value="">All statuses</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="PENDING_APPROVAL">PENDING_APPROVAL</option>
                <option value="REJECTED">REJECTED</option>
                <option value="ROLLBACK_REQUESTED">ROLLBACK_REQUESTED</option>
                <option value="ROLLBACK_REJECTED">ROLLBACK_REJECTED</option>
                <option value="REFUNDED">REFUNDED</option>
              </select>
              <input placeholder="Requester ID" value={requesterId} onChange={e => setRequesterId(e.target.value)} />
              <input placeholder="Operator ID" value={operatorId} onChange={e => setOperatorId(e.target.value)} />
            </div>
            <table>
              <thead>
              <tr>
                <th>Ref</th><th>Account</th><th>Payee</th><th>Amount</th><th>Status</th><th>Timestamp</th><th>Description</th><th>Action</th>
              </tr>
              </thead>
              <tbody>
              {filteredTransactions.map(t => (
                <tr key={t.id}>
                  <td>{t.reference}</td>
                  <td>{t.accountId}</td>
                  <td>{t.payeeId}</td>
                  <td>{t.amount} {t.currency}</td>
                  <td><span className={`badge ${t.status}`}>{t.status}</span></td>
                  <td>{new Date(t.timestamp).toLocaleString()}</td>
                  <td>{t.description}</td>
                  <td>
                    {t.status === 'PENDING_APPROVAL' ? (
                      <div className="row-actions">
                        <button onClick={() => reviewTransaction(t.id, true)}>Approve</button>
                        <button onClick={() => reviewTransaction(t.id, false)}>Reject</button>
                      </div>
                    ) : t.status === 'COMPLETED' ? (
                      <button onClick={() => requestRollback(t.id)}>Request Rollback</button>
                    ) : t.status === 'ROLLBACK_REQUESTED' ? (
                      <div className="row-actions">
                        <button onClick={() => reviewRollback(t.id, true)}>Approve Rollback</button>
                        <button onClick={() => reviewRollback(t.id, false)}>Reject Rollback</button>
                      </div>
                    ) : '-'}
                  </td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'alerts' && (
        <>
          <div className="panel">
            <h3>Alerts</h3>
            <div className="filters">
              <select value={alertFilter.status} onChange={e => setAlertFilter({ ...alertFilter, status: e.target.value })}>
                <option value="">All statuses</option>
                <option value="OPEN">OPEN</option>
                <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
                <option value="INVESTIGATING">INVESTIGATING</option>
                <option value="CLOSED">CLOSED</option>
                <option value="DISMISSED">DISMISSED</option>
              </select>
              <select value={alertFilter.severity} onChange={e => setAlertFilter({ ...alertFilter, severity: e.target.value })}>
                <option value="">All severities</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
              <select value={alertFilter.activeOnly ? 'true' : 'false'} onChange={e => setAlertFilter({ ...alertFilter, activeOnly: e.target.value === 'true' })}>
                <option value="true">Active only</option>
                <option value="false">All alerts</option>
              </select>
              <button onClick={loadAlerts}>Apply</button>
            </div>
            <table>
              <thead>
              <tr>
                <th>ID</th><th>Rule</th><th>Severity</th><th>Status</th><th>Created</th><th>Action</th>
              </tr>
              </thead>
              <tbody>
              {alerts.map(a => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td>{a.ruleName}</td>
                  <td>{a.severity}</td>
                  <td><span className={`badge ${a.status}`}>{a.status}</span></td>
                  <td>{new Date(a.createdAt).toLocaleString()}</td>
                  <td><button onClick={() => openAlert(a.id)}>Details</button></td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>

          {selectedAlert && (
            <div className="panel">
              <h3>Alert #{selectedAlert.id}</h3>
              <p><strong>Message:</strong> {selectedAlert.message}</p>
              <p><strong>Rule:</strong> {selectedAlert.ruleName} ({selectedAlert.ruleType})</p>
              <p><strong>Status:</strong> <span className={`badge ${selectedAlert.status}`}>{selectedAlert.status}</span></p>

              <div className="row-actions">
                <button onClick={() => updateAlertStatus('ACKNOWLEDGED')}>Acknowledge</button>
                <button onClick={() => updateAlertStatus('INVESTIGATING')}>Investigating</button>
                <button onClick={() => updateAlertStatus('CLOSED')}>Close</button>
                <button onClick={() => updateAlertStatus('DISMISSED')}>Dismiss</button>
              </div>

              <h4>Triggering Transactions</h4>
              <table>
                <thead><tr><th>Ref</th><th>Account</th><th>Payee</th><th>Amount</th><th>Time</th></tr></thead>
                <tbody>
                {selectedAlert.triggeringTransactions.map(t => (
                  <tr key={t.id}><td>{t.reference}</td><td>{t.accountId}</td><td>{t.payeeId}</td><td>{t.amount} {t.currency}</td><td>{new Date(t.timestamp).toLocaleString()}</td></tr>
                ))}
                </tbody>
              </table>

              <h4>History</h4>
              <table>
                <thead><tr><th>Time</th><th>From</th><th>To</th><th>Note</th></tr></thead>
                <tbody>
                {selectedAlert.history.map(h => (
                  <tr key={h.id}><td>{new Date(h.createdAt).toLocaleString()}</td><td>{h.fromStatus || '-'}</td><td>{h.toStatus}</td><td>{h.note}</td></tr>
                ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {tab === 'rules' && (
        <div className="panel">
          <h3>Monitoring Rules</h3>
          <table>
            <thead>
            <tr>
              <th>Name</th><th>Type</th><th>Severity</th><th>Active</th><th>Amount Threshold</th><th>Tx Count</th><th>Window (min)</th><th>Save</th>
            </tr>
            </thead>
            <tbody>
            {rules.map(rule => (
              <tr key={rule.id}>
                <td>{rule.name}</td>
                <td>{rule.type}</td>
                <td>
                  <select value={rule.severity} onChange={e => setRules(rules.map(r => r.id === rule.id ? { ...r, severity: e.target.value } : r))}>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </td>
                <td>
                  <input type="checkbox" checked={rule.active} onChange={e => setRules(rules.map(r => r.id === rule.id ? { ...r, active: e.target.checked } : r))} />
                </td>
                <td><input type="number" step="0.01" value={rule.amountThreshold || ''} onChange={e => setRules(rules.map(r => r.id === rule.id ? { ...r, amountThreshold: e.target.value === '' ? null : Number(e.target.value) } : r))} /></td>
                <td><input type="number" value={rule.transactionCountThreshold || ''} onChange={e => setRules(rules.map(r => r.id === rule.id ? { ...r, transactionCountThreshold: e.target.value === '' ? null : Number(e.target.value) } : r))} /></td>
                <td><input type="number" value={rule.timeWindowMinutes || ''} onChange={e => setRules(rules.map(r => r.id === rule.id ? { ...r, timeWindowMinutes: e.target.value === '' ? null : Number(e.target.value) } : r))} /></td>
                <td><button onClick={() => updateRule(rule)}>Save</button></td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);

