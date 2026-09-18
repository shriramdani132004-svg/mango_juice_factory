const API_BASE = '/api';

function getToken() {
  return localStorage.getItem('sf_token');
}

function setToken(token) {
  localStorage.setItem('sf_token', token);
}

function clearToken() {
  localStorage.removeItem('sf_token');
}

async function request(method, path, body = null) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, opts);
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }

  if (!res.ok) {
    const msg = typeof data === 'object' && data !== null ? (data.message || data.error || JSON.stringify(data)) : String(data);
    throw new Error(msg);
  }
  return data;
}

async function login(username, password) {
  const res = await request('POST', '/auth/login', { username, password });
  return res.data || res;
}

export const api = {
  login,
  me: () => request('GET', '/auth/me'),

  createOrder: (data) => request('POST', '/production-orders', data),
  getOrders: () => request('GET', '/production-orders'),
  getOrderDetail: (id) => request('GET', `/production-orders/${id}`),
  requirementsCheck: (id) => request('POST', `/production-orders/${id}/requirements-check`),
  approveOrder: (id) => request('POST', `/production-orders/${id}/approve`),
  cancelOrder: (id) => request('POST', `/production-orders/${id}/cancel`),
  getMachineCapacities: () => request('GET', '/production-orders/machine-capacities'),

  startPhase: (phaseId) => request('POST', `/production-phases/${phaseId}/start`),
  pausePhase: (phaseId) => request('POST', `/production-phases/${phaseId}/pause`),
  resumePhase: (phaseId) => request('POST', `/production-phases/${phaseId}/resume`),
  stopPhase: (phaseId) => request('POST', `/production-phases/${phaseId}/stop`),
  getPhaseCompletion: (phaseId) => request('GET', `/production-phases/${phaseId}/completion`),
  verifyPhase: (phaseId) => request('POST', `/production-phases/${phaseId}/verify`),
  getPhaseStatus: (phaseId) => request('GET', `/production-phases/${phaseId}/status`),

  getQualityChecksByBatch: (batchId) => request('GET', `/quality-checks/batch/${batchId}`),
  getQualityChecksByPhase: (phaseId) => request('GET', `/quality-checks/phase/${phaseId}`),
  getQualityChecksByOrder: (orderId) => request('GET', `/quality-checks/order/${orderId}`),
  getQualityCheck: (id) => request('GET', `/quality-checks/${id}`),
  inspectQualityCheck: (id, data) => request('POST', `/quality-checks/${id}/inspect`, data),
  autoEvaluateQualityCheck: (id) => request('POST', `/quality-checks/${id}/auto-evaluate`),
  createQualityChecksForPhase: (batchId, phaseId) => request('POST', `/quality-checks/batch/${batchId}/phase/${phaseId}/create`),
  getPhaseQualityResult: (batchId, phaseId) => request('GET', `/quality-checks/batch/${batchId}/phase/${phaseId}/result`),
  getBatchQualitySummary: (batchId) => request('GET', `/quality-checks/batch/${batchId}/summary`),
  quarantineBatch: (batchId, reason) => request('POST', `/quality-checks/batch/${batchId}/quarantine`, { reason }),
  rejectBatch: (batchId, reason) => request('POST', `/quality-checks/batch/${batchId}/reject`, { reason }),
  reprocessBatch: (batchId, reason) => request('POST', `/quality-checks/batch/${batchId}/reprocess`, { reason }),
  updatePhaseQualityStatus: (batchId, phaseId) => request('POST', `/quality-checks/batch/${batchId}/phase/${phaseId}/update-status`),

  getToken,
  setToken,
  clearToken,
};
