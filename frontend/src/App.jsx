import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from './api.js'
import './App.css'

function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.login(username, password)
      api.setToken(res.token)
      onLogin(res.user)
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>SmartFactory</h1>
        <p className="subtitle">Mango Juice Manufacturing Control System</p>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input value={username} onChange={e => setUsername(e.target.value)} placeholder="admin" autoFocus />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="admin123" />
          </div>
          {error && <p className="error-msg">{error}</p>}
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ width: '100%', marginTop: '1rem' }}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}

function NewOrderModal({ onClose, onCreated }) {
  const [products] = useState([{ id: 1, name: 'Mango Beverage', code: 'PROD-MANGO', bottleSizeMl: 500 }])
  const [productId, setProductId] = useState(1)
  const [mangoKg, setMangoKg] = useState('5000')
  const [bottleMl, setBottleMl] = useState('500')
  const [priority, setPriority] = useState('8')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    const qty = parseFloat(mangoKg)
    if (isNaN(qty) || qty <= 0) { setError('Quantity must be greater than 0'); return }
    setLoading(true)
    try {
      const order = await api.createOrder({
        productId: parseInt(productId),
        mangoQuantityKg: qty,
        bottleSizeMl: parseInt(bottleMl),
        priority: parseInt(priority),
        deadline: null
      })
      onCreated(order)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>New Production Order</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-group">
              <label>Product</label>
              <select value={productId} onChange={e => setProductId(e.target.value)}>
                {products.map(p => <option key={p.id} value={p.id}>{p.name} ({p.code})</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Mango Quantity (kg)</label>
              <input type="number" step="0.01" min="0.01" value={mangoKg} onChange={e => setMangoKg(e.target.value)} />
              <small style={{ color: '#888', fontSize: '0.75rem' }}>Existing stock: 50,000 kg</small>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Bottle Size (mL)</label>
                <select value={bottleMl} onChange={e => setBottleMl(e.target.value)}>
                  <option value="250">250 mL</option>
                  <option value="500">500 mL</option>
                  <option value="1000">1000 mL</option>
                </select>
              </div>
              <div className="form-group">
                <label>Priority (1-10)</label>
                <select value={priority} onChange={e => setPriority(e.target.value)}>
                  {[10,9,8,7,6,5,4,3,2,1].map(p => (
                    <option key={p} value={p}>{p} - {p >= 8 ? 'HIGH' : p >= 5 ? 'MEDIUM' : 'LOW'}</option>
                  ))}
                </select>
              </div>
            </div>
            {error && <p className="error-msg">{error}</p>}
          </div>
          <div className="modal-footer">
            <button className="btn btn-secondary" type="button" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Creating...' : 'Create Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function RequirementsPopup({ order, onClose, onApprove }) {
  const [plan, setPlan] = useState(null)
  const [loading, setLoading] = useState(true)
  const [approving, setApproving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const runCheck = async () => {
      try {
        const result = await api.requirementsCheck(order.id)
        setPlan(result)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    runCheck()
  }, [order.id])

  const handleApprove = async () => {
    setApproving(true)
    setError('')
    try {
      await api.approveOrder(order.id)
      onApprove()
    } catch (err) {
      setError(err.message)
    } finally {
      setApproving(false)
    }
  }

  if (loading) return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header"><h3>Running Requirements Check...</h3></div>
        <div className="modal-body"><p>Calculating materials, machines, and production plan...</p></div>
      </div>
    </div>
  )

  if (error) return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header"><h3>Error</h3><button className="modal-close" onClick={onClose}>&times;</button></div>
        <div className="modal-body"><p className="error-msg">{error}</p></div>
      </div>
    </div>
  )

  const materials = plan?.requirements?.filter(r => r.requirementType === 'MATERIAL') || []
  const machines = plan?.requirements?.filter(r => r.requirementType === 'MACHINE') || []
  const isBlocked = plan?.overallStatus === 'BLOCKED'

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Requirements Check — {order.orderNumber}</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <div className="modal-body">
          <div className="plan-summary">
            <div className="plan-stat"><div className="stat-value">{plan?.inputQuantity}</div><div className="stat-label">Input (kg)</div></div>
            <div className="plan-stat"><div className="stat-value">{plan?.batchCount}</div><div className="stat-label">Batches</div></div>
            <div className="plan-stat"><div className="stat-value">{plan?.expectedBottleCount?.toFixed(0)}</div><div className="stat-label">Expected Bottles</div></div>
            <div className="plan-stat"><div className="stat-value">{plan?.wastePercentage}%</div><div className="stat-label">Total Waste</div></div>
          </div>

          <h4 style={{ marginBottom: '0.5rem' }}>Material Requirements</h4>
          <div className="requirements-grid">
            {materials.map((r, i) => (
              <div key={i} className={`req-item req-${r.status}`}>
                <span className="req-icon">{r.status === 'GREEN' ? '\u2705' : r.status === 'YELLOW' ? '\u26A0\uFE0F' : '\u274C'}</span>
                <div>
                  <div className="req-name">{r.itemName} <span style={{ color: '#999', fontSize: '0.75rem' }}>({r.itemCode})</span></div>
                  <div className="req-detail">Need: {r.requiredQuantity} {r.requiredUnit} | Available: {r.availableQuantity} {r.requiredUnit}</div>
                  <div className="req-detail">{r.explanation}</div>
                </div>
                <span className="req-status" style={{ color: r.status === 'GREEN' ? '#27ae60' : r.status === 'YELLOW' ? '#f39c12' : '#e74c3c' }}>{r.status}</span>
              </div>
            ))}
          </div>

          <h4 style={{ margin: '1.2rem 0 0.5rem' }}>Machine Availability</h4>
          <div className="requirements-grid">
            {machines.map((r, i) => (
              <div key={i} className={`req-item req-${r.status}`}>
                <span className="req-icon">{r.status === 'GREEN' ? '\u2705' : '\u274C'}</span>
                <div>
                  <div className="req-name">{r.itemName}</div>
                  <div className="req-detail">{r.explanation}</div>
                </div>
                <span className="req-status" style={{ color: r.status === 'GREEN' ? '#27ae60' : '#e74c3c' }}>{r.status}</span>
              </div>
            ))}
          </div>

          <h4 style={{ margin: '1.2rem 0 0.5rem' }}>Production Phases</h4>
          <div className="phases-timeline">
            {plan?.phases?.map((p, i) => (
              <div key={i} className={`phase-item phase-${p.status}`}>
                <div className="phase-num">{p.phaseNumber}</div>
                <div>
                  <div className="phase-name">{p.phaseName}</div>
                  <div className="phase-meta">{p.requiredMachineCapability} | ~{p.estimatedDurationMinutes} min</div>
                </div>
                <span className="phase-status-label">{p.status}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Close</button>
          <button className="btn btn-success" onClick={handleApprove} disabled={approving || isBlocked}>
            {approving ? 'Approving...' : isBlocked ? 'Blocked (fix RED items)' : 'Approve Order'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ConfirmDialog({ title, message, confirmLabel, confirmClass, onConfirm, onCancel, loading }) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal modal-sm" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onCancel}>&times;</button>
        </div>
        <div className="modal-body">
          <p>{message}</p>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onCancel} disabled={loading}>Cancel</button>
          <button className={`btn ${confirmClass || 'btn-danger'}`} onClick={onConfirm} disabled={loading}>
            {loading ? 'Processing...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

function ActiveProduction({ orderId, onBack }) {
  const [order, setOrder] = useState(null)
  const [execution, setExecution] = useState(null)
  const [phaseStatus, setPhaseStatus] = useState(null)
  const [completion, setCompletion] = useState(null)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState('initial')
  const [actionLoading, setActionLoading] = useState(null)
  const [error, setError] = useState(null)
  const [sseConnected, setSseConnected] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [verifyResult, setVerifyResult] = useState(null)
  const [qualityChecks, setQualityChecks] = useState([])
  const [qualityResult, setQualityResult] = useState(null)
  const [qualityLoading, setQualityLoading] = useState(false)
  const eventSourceRef = useRef(null)
  const eventsRef = useRef([])
  const orderIdRef = useRef(orderId)
  const qualityLoadedRef = useRef(null)

  const currentPhase = order?.phases?.find(p => p.status === 'READY' || p.status === 'RUNNING' || p.status === 'PAUSED' || p.status === 'WAITING_FOR_VERIFICATION' || p.status === 'WAITING_FOR_QUALITY' || p.status === 'QUALITY_HOLD' || p.status === 'FAILED')
    || order?.phases?.find(p => p.status === 'LOCKED')
    || order?.phases?.[0]

  const handleSSEEvent = useCallback((event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.eventType === 'PHASE_PROGRESS' && data.data) {
        setExecution(data.data)
        setPhaseStatus({ executing: true, status: data.data.status, progress: data.data.progress, processedQuantity: data.data.processedQuantity, elapsedSeconds: data.data.elapsedSeconds })
      } else if (data.eventType === 'PHASE_STARTED') {
        setPhaseStatus(prev => ({ ...prev, executing: true, status: 'RUNNING' }))
        setExecution(prev => prev ? { ...prev, status: 'RUNNING' } : prev)
      } else if (data.eventType === 'PHASE_PAUSED') {
        setPhaseStatus(prev => ({ ...prev, executing: false, status: 'PAUSED' }))
        setExecution(prev => prev ? { ...prev, status: 'PAUSED' } : prev)
      } else if (data.eventType === 'PHASE_RESUMED') {
        setPhaseStatus(prev => ({ ...prev, executing: true, status: 'RUNNING' }))
        setExecution(prev => prev ? { ...prev, status: 'RUNNING' } : prev)
      } else if (data.eventType === 'PHASE_EMERGENCY_STOP') {
        setPhaseStatus({ executing: false, status: 'FAILED', progress: execution?.progress || 0 })
        setExecution(prev => prev ? { ...prev, status: 'FAILED' } : prev)
      } else if (data.eventType === 'PHASE_COMPLETED') {
        setPhaseStatus({ executing: false, status: 'WAITING_FOR_QUALITY', progress: 100 })
        setExecution(prev => prev ? { ...prev, status: 'WAITING_FOR_QUALITY', progress: 100 } : prev)
        if (currentPhase?.id) {
          api.getPhaseCompletion(currentPhase.id).then(c => setCompletion(c)).catch(() => {})
        }
      } else if (data.eventType === 'PHASE_VERIFIED') {
        setVerifyResult(data)
        setOrder(prev => prev ? { ...prev, status: 'READY' } : prev)
      } else if (data.eventType === 'QUALITY_CHECK_CREATED' || data.eventType === 'QUALITY_CHECK_PASSED' || data.eventType === 'QUALITY_CHECK_FAILED') {
        if (currentPhase?.id && currentPhase?.batchId) {
          loadQualityChecks(currentPhase.batchId, currentPhase.id)
        }
      }
      const eventEntry = { id: Date.now(), type: data.eventType, message: data.message, time: new Date().toLocaleTimeString() }
      eventsRef.current = [eventEntry, ...eventsRef.current].slice(0, 50)
      setEvents([...eventsRef.current])
    } catch { /* ignore parse errors */ }
  }, [execution, currentPhase])

  const openSSE = useCallback((oid) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }
    const es = new EventSource(`/api/production-phases/events/${oid}`)
    es.addEventListener('production-event', handleSSEEvent)
    es.onopen = () => setSseConnected(true)
    es.onerror = () => setSseConnected(false)
    eventSourceRef.current = es
  }, [handleSSEEvent])

  const loadQualityChecks = useCallback(async (batchId, phaseId) => {
    if (!batchId || !phaseId) return
    const key = `${batchId}-${phaseId}`
    if (qualityLoadedRef.current === key) return
    qualityLoadedRef.current = key
    try {
      const checks = await api.getQualityChecksByBatch(batchId)
      const phaseChecks = checks.filter(c => c.phaseId === phaseId)
      setQualityChecks(phaseChecks)
      const result = await api.getPhaseQualityResult(batchId, phaseId)
      setQualityResult(result)
    } catch { /* ignore */ }
  }, [])

  const handleAutoEvaluate = async (checkId) => {
    setQualityLoading(true)
    setError(null)
    try {
      const result = await api.autoEvaluateQualityCheck(checkId)
      setQualityChecks(prev => prev.map(c => c.id === checkId ? result : c))
      if (currentPhase?.batchId) {
        const qr = await api.getPhaseQualityResult(currentPhase.batchId, currentPhase.id)
        setQualityResult(qr)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setQualityLoading(false)
    }
  }

  const handleSimulateQualityFailure = async () => {
    if (!currentPhase?.batchId || qualityChecks.length === 0) return
    setQualityLoading(true)
    setError(null)
    try {
      const firstCheck = qualityChecks[0]
      await api.inspectQualityCheck(firstCheck.id, {
        observedValue: firstCheck.expectedMax ? firstCheck.expectedMax + 50 : 999,
        result: 'FAIL',
        notes: 'Simulated quality failure for demonstration'
      })
      const checks = await api.getQualityChecksByBatch(currentPhase.batchId)
      const phaseChecks = checks.filter(c => c.phaseId === currentPhase.id)
      setQualityChecks(phaseChecks)
      const qr = await api.getPhaseQualityResult(currentPhase.batchId, currentPhase.id)
      setQualityResult(qr)
      setPhaseStatus(prev => ({ ...prev, status: 'QUALITY_HOLD' }))
    } catch (err) {
      setError(err.message)
    } finally {
      setQualityLoading(false)
    }
  }

  const handleAutoPassAll = async () => {
    if (!qualityChecks.length) return
    setQualityLoading(true)
    setError(null)
    try {
      for (const check of qualityChecks) {
        if (!check.result) {
          const mid = check.expectedMin != null && check.expectedMax != null
            ? (Number(check.expectedMin) + Number(check.expectedMax)) / 2
            : 50
          await api.inspectQualityCheck(check.id, {
            observedValue: mid,
            result: 'PASS',
            notes: 'Auto-passed for demonstration'
          })
        }
      }
      const checks = await api.getQualityChecksByBatch(currentPhase.batchId)
      const phaseChecks = checks.filter(c => c.phaseId === currentPhase.id)
      setQualityChecks(phaseChecks)
      const qr = await api.getPhaseQualityResult(currentPhase.batchId, currentPhase.id)
      setQualityResult(qr)
      if (qr.overallResult === 'PASS') {
        await api.updatePhaseQualityStatus(currentPhase.batchId, currentPhase.id)
        setPhaseStatus(prev => ({ ...prev, status: 'WAITING_FOR_VERIFICATION' }))
        const detail = await api.getOrderDetail(orderIdRef.current)
        setOrder(detail)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setQualityLoading(false)
    }
  }

  const handleQuarantine = async (reason) => {
    if (!currentPhase?.batchId) return
    setActionLoading('quarantine')
    setError(null)
    try {
      await api.quarantineBatch(currentPhase.batchId, reason || 'Quality failure - quarantined')
      setPhaseStatus(prev => ({ ...prev, status: 'QUALITY_HOLD' }))
      const checks = await api.getQualityChecksByBatch(currentPhase.batchId)
      setQualityChecks(checks.filter(c => c.phaseId === currentPhase.id))
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
      setConfirmAction(null)
    }
  }

  const handleReject = async (reason) => {
    if (!currentPhase?.batchId) return
    setActionLoading('reject')
    setError(null)
    try {
      await api.rejectBatch(currentPhase.batchId, reason || 'Quality rejection')
      setPhaseStatus(prev => ({ ...prev, status: 'QUALITY_HOLD' }))
      const checks = await api.getQualityChecksByBatch(currentPhase.batchId)
      setQualityChecks(checks.filter(c => c.phaseId === currentPhase.id))
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
      setConfirmAction(null)
    }
  }

  const handleReprocess = async (reason) => {
    if (!currentPhase?.batchId) return
    setActionLoading('reprocess')
    setError(null)
    try {
      await api.reprocessBatch(currentPhase.batchId, reason || 'Reprocessing requested')
      const checks = await api.getQualityChecksByBatch(currentPhase.batchId)
      setQualityChecks(checks.filter(c => c.phaseId === currentPhase.id))
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
      setConfirmAction(null)
    }
  }

  useEffect(() => {
    orderIdRef.current = orderId
    qualityLoadedRef.current = null
    const loadOrder = async () => {
      try {
        const detail = await api.getOrderDetail(orderId)
        setOrder(detail)
        const runningPhase = detail.phases?.find(p => p.status === 'RUNNING' || p.status === 'PAUSED')
        const qualityPhase = detail.phases?.find(p => p.status === 'WAITING_FOR_QUALITY' || p.status === 'QUALITY_HOLD' || p.status === 'WAITING_FOR_VERIFICATION')
        const activePhase = runningPhase || qualityPhase
        if (activePhase) {
          if (runningPhase) {
            const st = await api.getPhaseStatus(runningPhase.id)
            if (st.executing) {
              setPhaseStatus(st)
              openSSE(orderId)
            }
          }
          if (qualityPhase?.batchId) {
            loadQualityChecks(qualityPhase.batchId, qualityPhase.id)
          }
        }
        setLoading(null)
      } catch (err) {
        setError(err.message)
        setLoading(null)
      }
    }
    loadOrder()
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
    }
  }, [orderId, openSSE, loadQualityChecks])

  useEffect(() => {
    if (execution && !eventSourceRef.current && execution.status === 'RUNNING') {
      openSSE(orderIdRef.current)
    }
  }, [execution, openSSE])

  const handleStart = async (phaseId) => {
    setActionLoading('starting')
    setError(null)
    try {
      const result = await api.startPhase(phaseId)
      setExecution(result)
      setPhaseStatus({ executing: true, status: 'RUNNING', progress: 0, processedQuantity: 0, elapsedSeconds: 0 })
      setOrder(prev => prev ? { ...prev, status: 'RUNNING' } : prev)
      openSSE(orderIdRef.current)
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
    }
  }

  const handlePause = async () => {
    if (!currentPhase) return
    setActionLoading('pausing')
    setError(null)
    try {
      await api.pausePhase(currentPhase.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
      setConfirmAction(null)
    }
  }

  const handleResume = async () => {
    if (!currentPhase) return
    setActionLoading('resuming')
    setError(null)
    try {
      await api.resumePhase(currentPhase.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
    }
  }

  const handleStop = async () => {
    if (!currentPhase) return
    setActionLoading('stopping')
    setError(null)
    try {
      await api.stopPhase(currentPhase.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
      setConfirmAction(null)
    }
  }

  const handleVerify = async () => {
    if (!currentPhase) return
    setActionLoading('verifying')
    setError(null)
    try {
      const result = await api.verifyPhase(currentPhase.id)
      setVerifyResult(result)
      const detail = await api.getOrderDetail(orderIdRef.current)
      setOrder(detail)
      setExecution(null)
      setPhaseStatus(null)
      setCompletion(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setActionLoading(null)
    }
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0s'
    const m = Math.floor(seconds / 60)
    const s = Math.floor(seconds % 60)
    return m > 0 ? `${m}m ${s}s` : `${s}s`
  }

  const fmt = (v) => v != null ? Number(v).toLocaleString(undefined, { maximumFractionDigits: 1 }) : '-'

  const fmtKg = (v) => v != null ? `${fmt(v)} kg` : '-'

  const phaseStatusDisplay = (s) => {
    switch (s) {
      case 'READY': return { label: 'READY', cls: 'status-ready', icon: '\u25B6' }
      case 'RUNNING': return { label: 'RUNNING', cls: 'status-running', icon: '\u25CF' }
      case 'PAUSED': return { label: 'PAUSED', cls: 'status-paused', icon: '\u275A\u275A' }
      case 'LOCKED': return { label: 'LOCKED', cls: 'status-locked', icon: '\u{1F512}' }
      case 'COMPLETED': return { label: 'COMPLETED', cls: 'status-completed', icon: '\u2714' }
      case 'WAITING_FOR_QUALITY': return { label: 'QUALITY CHECK', cls: 'status-verify', icon: '\u{1F50D}' }
      case 'QUALITY_HOLD': return { label: 'QUALITY HOLD', cls: 'status-failed', icon: '\u26A0\uFE0F' }
      case 'WAITING_FOR_VERIFICATION': return { label: 'AWAITING VERIFY', cls: 'status-verify', icon: '\u23F3' }
      case 'VERIFIED': return { label: 'VERIFIED', cls: 'status-verified', icon: '\u2705' }
      case 'FAILED': return { label: 'STOPPED', cls: 'status-failed', icon: '\u{1F534}' }
      default: return { label: s || 'UNKNOWN', cls: 'status-locked', icon: '\u2022' }
    }
  }

  if (loading === 'initial') return (
    <div className="active-production">
      <div className="loading-overlay">
        <div className="loading-spinner" />
        <p>Loading production data...</p>
      </div>
    </div>
  )

  if (error && !order) return (
    <div className="active-production">
      <div className="loading-overlay">
        <p className="error-msg">{error}</p>
        <button className="btn btn-secondary" onClick={onBack}>Back to Dashboard</button>
      </div>
    </div>
  )

  const exec = execution
  const inputQty = exec?.inputQuantity ?? order?.inputQuantity
  const processedQty = exec?.processedQuantity ?? 0
  const outputQty = exec?.outputQuantity ?? 0
  const wasteQty = exec?.wasteQuantity ?? 0
  const progress = exec?.progress ?? phaseStatus?.progress ?? 0
  const rate = exec?.productionRate ?? 0
  const elapsed = exec?.elapsedSeconds ?? phaseStatus?.elapsedSeconds ?? 0
  const remaining = exec?.remainingSeconds ?? 0
  const machines = exec?.assignedMachines ?? []
  const telemetry = exec?.telemetry ?? []
  const activeStatus = exec?.status ?? phaseStatus?.status ?? currentPhase?.status ?? 'UNKNOWN'

  const isRunning = activeStatus === 'RUNNING'
  const isPaused = activeStatus === 'PAUSED'
  const isWaitingQuality = activeStatus === 'WAITING_FOR_QUALITY'
  const isQualityHold = activeStatus === 'QUALITY_HOLD'
  const isWaitingVerify = activeStatus === 'WAITING_FOR_VERIFICATION'
  const isCompleted = isWaitingVerify || activeStatus === 'COMPLETED'
  const isFailed = activeStatus === 'FAILED' || activeStatus === 'EMERGENCY_STOP'
  const isReady = activeStatus === 'READY' && !isWaitingQuality && !isQualityHold && !isWaitingVerify
  const canStart = isReady && !actionLoading
  const canPause = isRunning && !actionLoading
  const canResume = isPaused && !actionLoading
  const canStop = (isRunning || isPaused) && !actionLoading
  const canVerify = isWaitingVerify && !actionLoading && qualityResult?.overallResult === 'PASS'

  return (
    <div className="active-production">
      <header className="app-header">
        <div className="header-left">
          <button className="btn btn-ghost" onClick={onBack}>\u2190 Dashboard</button>
          <h2>SmartFactory</h2>
        </div>
        <div className="user-info">
          {sseConnected ? <span className="sse-indicator sse-connected">LIVE</span> : <span className="sse-indicator sse-disconnected">OFFLINE</span>}
        </div>
      </header>

      {error && <div className="error-banner">{error}<button onClick={() => setError(null)}>&times;</button></div>}

      <div className="production-layout">
        <div className="production-main">
          <div className="order-header-card">
            <div>
              <h3>{order?.productName || 'Production'}</h3>
              <p className="order-num">{order?.orderNumber}</p>
            </div>
            <div className="order-meta">
              <span>{fmtKg(inputQty)} input</span>
              <span>{order?.estimatedOutputQty ? `${fmt(order.estimatedOutputQty)} btl expected` : ''}</span>
            </div>
          </div>

          <div className="phase-pipeline">
            {order?.phases?.map((p) => {
              const ps = phaseStatusDisplay(p.status)
              return (
                <div key={p.id} className={`pipeline-item pipeline-${p.status}`}>
                  <div className={`pipeline-num ${p.status === 'RUNNING' ? 'pulse' : ''}`}>{p.phaseNumber}</div>
                  <div className="pipeline-info">
                    <span className="pipeline-name">{p.phaseName}</span>
                    <span className="pipeline-meta">{p.requiredMachineCapability}</span>
                  </div>
                  <span className={`pipeline-status ${ps.cls}`}>{ps.icon} {ps.label}</span>
                </div>
              )
            })}
          </div>

          {(isRunning || isPaused || isFailed) && (
            <div className="current-phase-card">
              <div className="phase-card-header">
                <div>
                  <h4>Phase {currentPhase?.phaseNumber} of 10</h4>
                  <h3>{exec?.phaseName || currentPhase?.phaseName}</h3>
                </div>
                <div className={`phase-live-badge ${activeStatus.toLowerCase()}`}>
                  {activeStatus === 'RUNNING' && <span className="live-dot" />}
                  {activeStatus}
                </div>
              </div>

              <div className="progress-section">
                <div className="progress-header">
                  <span>Progress</span>
                  <span className="progress-pct">{fmt(progress)}%</span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${Math.min(progress, 100)}%` }} />
                </div>
              </div>

              <div className="summary-grid">
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(inputQty)}</div>
                  <div className="summary-label">Input</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(processedQty)}</div>
                  <div className="summary-label">Processed</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(outputQty || (processedQty * 0.98))}</div>
                  <div className="summary-label">Output</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(wasteQty || (processedQty * 0.02))}</div>
                  <div className="summary-label">Waste</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmt(rate)} kg/s</div>
                  <div className="summary-label">Rate</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{formatTime(elapsed)}</div>
                  <div className="summary-label">Elapsed</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{isRunning ? formatTime(remaining) : '-'}</div>
                  <div className="summary-label">Remaining</div>
                </div>
              </div>
            </div>
          )}

          {isReady && !isCompleted && !isFailed && !exec && (
            <div className="current-phase-card">
              <div className="phase-card-header">
                <div>
                  <h4>Phase {currentPhase?.phaseNumber} of 10</h4>
                  <h3>{currentPhase?.phaseName}</h3>
                </div>
                <div className="phase-live-badge ready">READY</div>
              </div>
              <div className="start-prompt">
                <p>Phase is ready to begin production.</p>
                <button className="btn btn-primary btn-lg" onClick={() => handleStart(currentPhase?.id)} disabled={!canStart}>
                  {actionLoading === 'starting' ? 'Starting...' : '\u25B6 Start Phase'}
                </button>
              </div>
            </div>
          )}

          {(isWaitingQuality || isQualityHold || isWaitingVerify || (isCompleted && completion)) && completion && (
            <div className={`completion-card ${isQualityHold ? 'failed-card' : ''}`}>
              <div className="completion-header">
                <h3>Phase Complete</h3>
                <span className={`status-badge ${isQualityHold ? 'status-failed' : isWaitingVerify ? 'status-verify' : 'status-verify'}`}>
                  {isQualityHold ? 'QUALITY HOLD' : isWaitingVerify ? 'AWAITING VERIFICATION' : 'QUALITY CHECK IN PROGRESS'}
                </span>
              </div>
              <div className="summary-grid">
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(completion.inputQuantity)}</div>
                  <div className="summary-label">Input</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(completion.processedQuantity)}</div>
                  <div className="summary-label">Processed</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(completion.outputQuantity)}</div>
                  <div className="summary-label">Output</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{fmtKg(completion.wasteQuantity)}</div>
                  <div className="summary-label">Waste</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{completion.machinesUsed}</div>
                  <div className="summary-label">Machines</div>
                </div>
                <div className="summary-item">
                  <div className="summary-value">{formatTime(completion.processingTimeSeconds)}</div>
                  <div className="summary-label">Duration</div>
                </div>
              </div>
            </div>
          )}

          {qualityChecks.length > 0 && (
            <div className={`completion-card ${qualityResult?.overallResult === 'FAIL' ? 'failed-card' : ''}`}>
              <div className="completion-header">
                <h3>Quality Checks</h3>
                <span className={`status-badge ${qualityResult?.overallResult === 'PASS' ? 'status-completed' : qualityResult?.overallResult === 'FAIL' ? 'status-failed' : 'status-verify'}`}>
                  {qualityResult?.overallResult === 'PASS' ? '\u2705 ALL PASSED' : qualityResult?.overallResult === 'FAIL' ? '\u274C FAILED' : '\u23F3 PENDING'}
                </span>
              </div>

              <div className="quality-checks-list">
                {qualityChecks.map(check => (
                  <div key={check.id} className={`quality-check-item ${check.result === 'PASS' ? 'qc-pass' : check.result === 'FAIL' ? 'qc-fail' : 'qc-pending'}`}>
                    <div className="qc-header">
                      <span className="qc-param">{check.parameterName}</span>
                      <span className={`qc-result ${check.result === 'PASS' ? 'result-pass' : check.result === 'FAIL' ? 'result-fail' : 'result-pending'}`}>
                        {check.result === 'PASS' ? '\u2705 PASS' : check.result === 'FAIL' ? '\u274C FAIL' : check.mandatory ? '\u23F3 PENDING' : '\u2014 OPTIONAL'}
                      </span>
                    </div>
                    <div className="qc-details">
                      <span>Type: {check.checkType?.replace(/_/g, ' ')}</span>
                      {check.observedValue != null && <span>Observed: {Number(check.observedValue).toFixed(2)} {check.unit || ''}</span>}
                      <span>Expected: {check.expectedMin != null && check.expectedMax != null ? `${check.expectedMin} - ${check.expectedMax} ${check.unit || ''}` : 'Manual'}</span>
                    </div>
                    {check.notes && <div className="qc-notes">Notes: {check.notes}</div>}
                    {!check.result && check.mandatory && (
                      <button className="btn btn-sm btn-primary" onClick={() => handleAutoEvaluate(check.id)} disabled={qualityLoading} style={{marginTop: '0.3rem'}}>
                        {qualityLoading ? 'Evaluating...' : 'Evaluate'}
                      </button>
                    )}
                  </div>
                ))}
              </div>

              <div className="quality-actions" style={{marginTop: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap'}}>
                {qualityResult?.overallResult !== 'PASS' && qualityResult?.overallResult !== 'FAIL' && (
                  <button className="btn btn-success btn-sm" onClick={handleAutoPassAll} disabled={qualityLoading}>
                    {qualityLoading ? 'Processing...' : '\u2705 Auto-Pass All'}
                  </button>
                )}
                {qualityResult?.overallResult === 'PASS' && isWaitingVerify && (
                  <button className="btn btn-success btn-lg" onClick={handleVerify} disabled={!canVerify || !!actionLoading}>
                    {actionLoading === 'verifying' ? 'Verifying...' : '\u2714 Verify Phase'}
                  </button>
                )}
                {qualityResult?.overallResult === 'FAIL' && (
                  <>
                    <button className="btn btn-warning btn-sm" onClick={() => setConfirmAction('reprocess')} disabled={!!actionLoading}>Reprocess</button>
                    <button className="btn btn-secondary btn-sm" onClick={() => setConfirmAction('quarantine')} disabled={!!actionLoading}>Quarantine</button>
                    <button className="btn btn-danger btn-sm" onClick={() => setConfirmAction('reject')} disabled={!!actionLoading}>Reject</button>
                  </>
                )}
                <button className="btn btn-danger btn-sm" onClick={handleSimulateQualityFailure} disabled={qualityLoading} style={{marginLeft: 'auto'}}>
                  {qualityLoading ? 'Simulating...' : '\u26A0 Simulate Failure'}
                </button>
              </div>
            </div>
          )}

          {isFailed && (
            <div className="completion-card failed-card">
              <div className="completion-header">
                <h3>Emergency Stop</h3>
                <span className="status-badge status-failed">STOPPED</span>
              </div>
              <p>Active machine execution was interrupted. Phase must be restarted or investigated.</p>
            </div>
          )}

          {verifyResult && (
            <div className="verify-result-card">
              <div className="completion-header">
                <h3>Phase Verified</h3>
                <span className="status-badge status-verified">VERIFIED</span>
              </div>
              <p>{verifyResult.message}</p>
              {verifyResult.nextPhaseNumber && (
                <p className="next-phase-info">Phase {verifyResult.nextPhaseNumber} is now <strong>READY</strong>.</p>
              )}
              {verifyResult.orderStatus === 'COMPLETED' && (
                <p className="next-phase-info">All phases complete! Order is <strong>COMPLETED</strong>.</p>
              )}
            </div>
          )}

          <div className="controls-bar">
            {canPause && (
              <button className="btn btn-warning" onClick={() => setConfirmAction('pause')} disabled={!!actionLoading}>
                {'\u275A\u275A'} Pause
              </button>
            )}
            {canResume && (
              <button className="btn btn-primary" onClick={handleResume} disabled={!!actionLoading}>
                {actionLoading === 'resuming' ? 'Resuming...' : '\u25B6 Resume'}
              </button>
            )}
            {canStop && (
              <button className="btn btn-danger" onClick={() => setConfirmAction('stop')} disabled={!!actionLoading}>
                {'\u23F9'} Emergency Stop
              </button>
            )}
          </div>
        </div>

        <div className="production-sidebar">
          {machines.length > 0 && (
            <div className="sidebar-section">
              <h4>Machines ({machines.length})</h4>
              <div className="machine-list">
                {machines.map(m => (
                  <div key={m.machineId} className={`machine-card machine-${m.status?.toLowerCase()}`}>
                    <div className="machine-header">
                      <span className="machine-code">{m.machineCode}</span>
                      <span className={`machine-status-badge status-${m.status?.toLowerCase()}`}>{m.status}</span>
                    </div>
                    <div className="machine-detail">{m.capability}</div>
                    <div className="machine-stats">
                      <div className="machine-stat">
                        <span className="stat-lbl">Health</span>
                        <span className="stat-val">{m.healthScore}%</span>
                      </div>
                      <div className="machine-stat">
                        <span className="stat-lbl">Temp</span>
                        <span className="stat-val">{m.temperature ? `${fmt(m.temperature)}\u00B0C` : '-'}</span>
                      </div>
                      <div className="machine-stat">
                        <span className="stat-lbl">RPM</span>
                        <span className="stat-val">{m.rpm ? fmt(m.rpm) : '-'}</span>
                      </div>
                      <div className="machine-stat">
                        <span className="stat-lbl">Vib</span>
                        <span className="stat-val">{m.vibration ? `${fmt(m.vibration)}mm/s` : '-'}</span>
                      </div>
                      <div className="machine-stat">
                        <span className="stat-lbl">Power</span>
                        <span className="stat-val">{m.powerConsumption ? `${fmt(m.powerConsumption)}kW` : '-'}</span>
                      </div>
                      <div className="machine-stat">
                        <span className="stat-lbl">Rate</span>
                        <span className="stat-val">{fmt(m.productionRate)}/s</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {telemetry.length > 0 && (
            <div className="sidebar-section">
              <h4>Live Telemetry</h4>
              <div className="telemetry-grid">
                {telemetry.map(t => (
                  <div key={t.machineId} className="telemetry-row">
                    <span className="telem-machine">{t.machineCode}</span>
                    <div className="telem-values">
                      <span title="Temperature">{t.temperature ? `${fmt(t.temperature)}\u00B0` : '-'}</span>
                      <span title="RPM">{t.rpm ? `${fmt(t.rpm)}rpm` : '-'}</span>
                      <span title="Vibration">{t.vibration ? `${fmt(t.vibration)}mm` : '-'}</span>
                      <span title="Power">{t.powerConsumption ? `${fmt(t.powerConsumption)}kW` : '-'}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="sidebar-section">
            <h4>Event Stream</h4>
            <div className="event-stream">
              {events.length === 0 ? (
                <p className="event-empty">Waiting for events...</p>
              ) : (
                events.map(ev => (
                  <div key={ev.id} className={`event-item event-${ev.type?.toLowerCase()?.includes('error') || ev.type?.includes('STOP') ? 'error' : ev.type?.includes('PROGRESS') ? 'info' : 'normal'}`}>
                    <span className="event-time">{ev.time}</span>
                    <span className="event-msg">{ev.message}</span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {confirmAction === 'pause' && (
        <ConfirmDialog
          title="Pause Production?"
          message="Current progress will be preserved."
          confirmLabel="Pause"
          confirmClass="btn-warning"
          onConfirm={handlePause}
          onCancel={() => setConfirmAction(null)}
          loading={!!actionLoading}
        />
      )}
      {confirmAction === 'stop' && (
        <ConfirmDialog
          title="Emergency Stop?"
          message="Active machine execution will be interrupted. This cannot be undone."
          confirmLabel="Confirm Emergency Stop"
          confirmClass="btn-danger"
          onConfirm={handleStop}
          onCancel={() => setConfirmAction(null)}
          loading={!!actionLoading}
        />
      )}
      {confirmAction === 'quarantine' && (
        <ConfirmDialog
          title="Quarantine Batch?"
          message="The batch will be placed in quarantine. No further production can proceed until resolved."
          confirmLabel="Quarantine"
          confirmClass="btn-secondary"
          onConfirm={() => handleQuarantine('Quality failure - quarantined by admin')}
          onCancel={() => setConfirmAction(null)}
          loading={!!actionLoading}
        />
      )}
      {confirmAction === 'reject' && (
        <ConfirmDialog
          title="Reject Batch?"
          message="The batch will be rejected. Rejected quantity will be recorded as inventory REJECTION. This cannot be undone."
          confirmLabel="Reject Batch"
          confirmClass="btn-danger"
          onConfirm={() => handleReject('Quality failure - rejected by admin')}
          onCancel={() => setConfirmAction(null)}
          loading={!!actionLoading}
        />
      )}
      {confirmAction === 'reprocess' && (
        <ConfirmDialog
          title="Request Reprocessing?"
          message="The batch will be flagged for reprocessing. History will be preserved. The batch can return to the appropriate production step."
          confirmLabel="Reprocess"
          confirmClass="btn-warning"
          onConfirm={() => handleReprocess('Reprocessing requested by admin after quality failure')}
          onCancel={() => setConfirmAction(null)}
          loading={!!actionLoading}
        />
      )}
    </div>
  )
}

function Dashboard({ user, onLogout, onStartProduction }) {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [showNewOrder, setShowNewOrder] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState(null)

  const loadOrders = useCallback(async () => {
    try {
      const data = await api.getOrders()
      setOrders(data)
    } catch (err) {
      if (err.message.includes('401') || err.message.includes('Unauthorized')) {
        onLogout()
      }
    } finally {
      setLoading(false)
    }
  }, [onLogout])

  useEffect(() => { loadOrders() }, [loadOrders])

  const handleOrderCreated = (order) => {
    setShowNewOrder(false)
    setSelectedOrder(order)
    loadOrders()
  }

  const handleOrderApproved = () => {
    setSelectedOrder(null)
    loadOrders()
  }

  const handleViewOrder = async (orderId) => {
    try {
      const detail = await api.getOrderDetail(orderId)
      setSelectedOrder(detail)
    } catch (err) { /* ignore */ }
  }

  const priorityLabel = (p) => p >= 8 ? 'HIGH' : p >= 5 ? 'MED' : 'LOW'
  const priorityClass = (p) => p >= 8 ? 'priority-high' : p >= 5 ? 'priority-medium' : 'priority-low'

  return (
    <div>
      <header className="app-header">
        <h2>SmartFactory</h2>
        <div className="user-info">
          <span>{user.displayName || user.username}</span>
          <span className="role-badge">{user.role}</span>
          <button className="btn btn-secondary btn-sm" onClick={onLogout}>Logout</button>
        </div>
      </header>

      <div className="dashboard">
        <div className="dashboard-header">
          <h3>Production Orders</h3>
          <button className="btn btn-primary" onClick={() => setShowNewOrder(true)}>+ New Order</button>
        </div>

        {loading ? <p>Loading...</p> : orders.length === 0 ? (
          <div className="empty-state"><h4>No production orders yet</h4><p>Click "+ New Order" to create your first production order.</p></div>
        ) : (
          <table className="orders-table">
            <thead>
              <tr>
                <th>Order #</th>
                <th>Product</th>
                <th>Input</th>
                <th>Output</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {orders.map(order => (
                <tr key={order.id}>
                  <td><strong>{order.orderNumber}</strong></td>
                  <td>{order.productName}</td>
                  <td>{order.inputQuantity} kg</td>
                  <td>{order.estimatedOutputQty ? `${Number(order.estimatedOutputQty).toFixed(0)} btl` : '-'}</td>
                  <td><span className={`priority-badge ${priorityClass(order.priority)}`}>{priorityLabel(order.priority)}</span></td>
                  <td><span className={`status-badge status-${order.status}`}>{order.status}</span></td>
                  <td>
                    {(order.status === 'READY' || order.status === 'RUNNING' || order.status === 'PAUSED') ? (
                      <button className="btn btn-primary btn-sm" onClick={() => onStartProduction(order.id)}>
                        {order.status === 'READY' ? 'Start' : 'View'}
                      </button>
                    ) : order.status === 'DRAFT' ? (
                      <button className="btn btn-primary btn-sm" onClick={() => handleViewOrder(order.id)}>Check</button>
                    ) : (
                      <button className="btn btn-secondary btn-sm" onClick={() => handleViewOrder(order.id)}>View</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showNewOrder && <NewOrderModal onClose={() => setShowNewOrder(false)} onCreated={handleOrderCreated} />}
      {selectedOrder && <RequirementsPopup order={selectedOrder} onClose={() => setSelectedOrder(null)} onApprove={handleOrderApproved} />}
    </div>
  )
}

function App() {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)
  const [activeOrderId, setActiveOrderId] = useState(null)

  useEffect(() => {
    const token = api.getToken()
    if (!token) { setChecking(false); return }
    api.me().then(u => setUser(u)).catch(() => api.clearToken()).finally(() => setChecking(false))
  }, [])

  const handleLogout = () => {
    api.clearToken()
    setUser(null)
    setActiveOrderId(null)
  }

  if (checking) return <div style={{ textAlign: 'center', padding: '4rem', color: '#999' }}>Loading...</div>
  if (!user) return <LoginPage onLogin={setUser} />
  if (activeOrderId) return <ActiveProduction orderId={activeOrderId} onBack={() => setActiveOrderId(null)} />
  return <Dashboard user={user} onLogout={handleLogout} onStartProduction={setActiveOrderId} />
}

export default App
