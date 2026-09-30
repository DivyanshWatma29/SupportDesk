import { useEffect, useState } from "react";
import {
  checkAuth,
  checkHealth,
  createTicket,
  deleteTicket,
  getDashboardSummary,
  getDepartments,
  getTicketDetails,
  getTickets,
  trackTicket,
  updateTicket
} from "./api";

const priorities = ["LOW", "MEDIUM", "HIGH"];
const statuses = ["OPEN", "IN_PROGRESS", "RESOLVED"];

const DEFAULT_DEPARTMENTS = [
  { id: 1, name: "IT Support", code: "IT" },
  { id: 2, name: "Staff Support (HR)", code: "HR" },
  { id: 3, name: "Fees & Finance", code: "FIN" },
  { id: 4, name: "Campus Facilities", code: "FAC" },
  { id: 5, name: "ID Cards & Access", code: "SEC" }
];

function readable(value) {
  if (!value) return "";
  return value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function TicketForm({ departments = DEFAULT_DEPARTMENTS }) {
  const depts = departments && departments.length > 0 ? departments : DEFAULT_DEPARTMENTS;
  const [ticket, setTicket] = useState({
    requesterName: "",
    requesterEmail: "",
    departmentId: depts[0]?.id || 1,
    title: "",
    description: "",
    priority: "MEDIUM"
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (depts.length > 0 && !ticket.departmentId) {
      setTicket((prev) => ({ ...prev, departmentId: depts[0].id }));
    }
  }, [depts]);

  function changeField(event) {
    setTicket({ ...ticket, [event.target.name]: event.target.value });
  }

  async function submitTicket(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    setError("");
    try {
      const payload = {
        ...ticket,
        departmentId: Number(ticket.departmentId)
      };
      const created = await createTicket(payload);
      setMessage(`Ticket ${created.ticketNumber} was submitted successfully. You can track its progress in the Track Ticket tab.`);
      setTicket({
        requesterName: "",
        requesterEmail: "",
        departmentId: depts[0]?.id || 1,
        title: "",
        description: "",
        priority: "MEDIUM"
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="content-grid">
      <div className="intro-card">
        <p className="eyebrow">Campus Service Desk</p>
        <h2>Report an IT or Campus Issue</h2>
        <p>Students and staff can report an IT or campus problem here. It is sent to the right department.</p>
        <div className="support-note">
          <strong>No login needed:</strong> Your ticket is saved with its department, priority, and status. Keep the ticket number to track it.
        </div>
      </div>

      <form className="card ticket-form" onSubmit={submitTicket}>
        <h2>Create a Ticket</h2>
        {message && <p className="message success">{message}</p>}
        {error && <p className="message error">{error}</p>}

        <div className="form-row">
          <label>
            Your Name
            <input
              name="requesterName"
              value={ticket.requesterName}
              onChange={changeField}
              maxLength="100"
              placeholder="Full name"
              required
            />
          </label>
          <label>
            Email Address
            <input
              type="email"
              name="requesterEmail"
              value={ticket.requesterEmail}
              onChange={changeField}
              maxLength="255"
              placeholder="Email address"
              required
            />
          </label>
        </div>

        <div className="form-row">
          <label>
            Assigned Department
            <select name="departmentId" value={ticket.departmentId} onChange={changeField} required>
              {depts.map((dept) => (
                <option key={dept.id} value={dept.id}>
                  {dept.name} ({dept.code})
                </option>
              ))}
            </select>
          </label>
          <label>
            Priority
            <select name="priority" value={ticket.priority} onChange={changeField}>
              {priorities.map((priority) => (
                <option key={priority} value={priority}>{readable(priority)}</option>
              ))}
            </select>
          </label>
        </div>

        <label>
          Issue Title
          <input
            name="title"
            value={ticket.title}
            onChange={changeField}
            minLength="4"
            maxLength="150"
            placeholder="Brief summary of the issue"
            required
          />
        </label>

        <label>
          Describe the Issue
          <textarea
            name="description"
            value={ticket.description}
            onChange={changeField}
            minLength="10"
            maxLength="2000"
            placeholder="Provide full details, error messages, and steps to reproduce..."
            required
          />
        </label>

        <button className="primary-button" disabled={submitting}>
          {submitting ? "Submitting..." : "Submit Ticket"}
        </button>
      </form>
    </section>
  );
}

function TrackTicketTab() {
  const [ticketNumber, setTicketNumber] = useState("");
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState("");
  const [searching, setSearching] = useState(false);

  async function handleSearch(event) {
    event.preventDefault();
    if (!ticketNumber.trim()) return;
    setSearching(true);
    setError("");
    setDetail(null);
    try {
      const data = await trackTicket(ticketNumber.trim());
      setDetail(data);
    } catch (err) {
      setError(err.message || "Ticket not found. Please verify the ticket number.");
    } finally {
      setSearching(false);
    }
  }

  return (
    <section className="track-section">
      <div className="track-header">
        <p className="eyebrow">Requester Portal</p>
        <h2>Track Your Support Ticket</h2>
        <p className="muted">Enter your ticket number to check its current status and update history.</p>
      </div>

      <form className="card track-form" onSubmit={handleSearch}>
        <input
          value={ticketNumber}
          onChange={(e) => setTicketNumber(e.target.value)}
          placeholder="Enter Ticket Number"
          required
        />
        <button className="primary-button" disabled={searching}>
          {searching ? "Searching..." : "Track Status"}
        </button>
      </form>

      {error && <p className="message error" style={{ marginTop: "16px" }}>{error}</p>}

      {detail && (
        <article className="card track-result">
          <div className="result-header">
            <div>
              <span className="eyebrow">{detail.ticket.departmentName} ({detail.ticket.departmentCode})</span>
              <h3>{detail.ticket.title}</h3>
              <p className="muted">
                Ticket: <strong>{detail.ticket.ticketNumber}</strong> · Raised by {detail.ticket.requesterName} ({detail.ticket.requesterEmail})
              </p>
            </div>
            <div className="ticket-meta">
              <span className={`badge ${detail.ticket.priority.toLowerCase()}`}>{readable(detail.ticket.priority)}</span>
              <span className={`badge status-${detail.ticket.status.toLowerCase()}`}>{readable(detail.ticket.status)}</span>
            </div>
          </div>

          <div className="track-body">
            <p><strong>Description:</strong> {detail.ticket.description}</p>
            {detail.ticket.adminNote && (
              <div className="support-note" style={{ marginTop: "12px" }}>
                <strong>Resolution Note:</strong> {detail.ticket.adminNote}
              </div>
            )}
          </div>

          <div className="timeline-section">
            <h4>Ticket Update History</h4>
            <div className="timeline">
              {detail.timeline.map((log) => (
                <div key={log.id} className="timeline-item">
                  <div className="timeline-marker" />
                  <div className="timeline-content">
                    <div className="timeline-title">
                      <strong>{readable(log.action)}</strong>
                      <small className="muted">{formatDate(log.createdAt)}</small>
                    </div>
                    <p className="muted">
                      {log.oldStatus ? `${readable(log.oldStatus)} → ` : ""}
                      <span className="highlight-status">{readable(log.newStatus)}</span>
                      {log.changedByName && ` · by ${log.changedByName}`}
                    </p>
                    {log.note && <p className="timeline-note">{log.note}</p>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </article>
      )}
    </section>
  );
}

// Public demo accounts for the live demo only. Data is temporary and resets on restart.
const DEMO_ACCOUNTS = {
  admin: { username: "admin", password: "DemoAdmin2026!", label: "Administrator" },
  agent: { username: "agent", password: "DemoAgent2026!", label: "Support Agent" }
};

function LoginModal({ onClose, onLoginSuccess }) {
  const [form, setForm] = useState({
    username: DEMO_ACCOUNTS.admin.username,
    password: DEMO_ACCOUNTS.admin.password
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleLogin(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const auth = await checkAuth(form);
      onLoginSuccess({
        username: auth.username,
        role: auth.role,
        credentials: { ...form }
      });
      onClose();
    } catch (err) {
      setError(err.message || "Invalid username or password.");
    } finally {
      setLoading(false);
    }
  }

  function selectDemoAccount(role) {
    const account = DEMO_ACCOUNTS[role] || DEMO_ACCOUNTS.agent;
    setForm({ username: account.username, password: account.password });
    setError("");
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content login-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Staff Sign In</h3>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>
        <p className="muted" style={{ margin: "4px 0 12px" }}>Demo login for the live project. Pick a role, then press Sign In.</p>
        <div className="message" style={{ marginBottom: "14px", fontSize: "0.9rem", lineHeight: 1.6 }}>
          <strong>Demo accounts</strong><br />
          Admin: <code>{DEMO_ACCOUNTS.admin.username}</code> / <code>{DEMO_ACCOUNTS.admin.password}</code><br />
          Agent: <code>{DEMO_ACCOUNTS.agent.username}</code> / <code>{DEMO_ACCOUNTS.agent.password}</code>
        </div>

        <div className="demo-roles">
          <button
            type="button"
            className={`role-tab ${form.username === "admin" ? "active" : ""}`}
            onClick={() => selectDemoAccount("admin")}
          >
            Administrator
          </button>
          <button
            type="button"
            className={`role-tab ${form.username === "agent" ? "active" : ""}`}
            onClick={() => selectDemoAccount("agent")}
          >
            Support Agent
          </button>
        </div>

        {error && <p className="message error" style={{ marginBottom: "14px" }}>{error}</p>}

        <form onSubmit={handleLogin} style={{ display: "grid", gap: "14px" }}>
          <label>
            Username
            <input
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </label>
          <label>
            Password
            <input
              type={showPassword ? "text" : "password"}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Password"
              autoComplete="current-password"
              required
            />
          </label>
          <button
            type="button"
            onClick={() => setShowPassword((value) => !value)}
            aria-pressed={showPassword}
            style={{ justifySelf: "start", background: "none", border: "none", padding: 0, color: "#1d4ed8", cursor: "pointer", fontSize: "0.9rem", fontWeight: 600 }}
          >
            {showPassword ? "Hide password" : "Show password"}
          </button>
          <button className="primary-button" disabled={loading} style={{ marginTop: "6px" }}>
            {loading ? "Signing in..." : `Sign in as ${form.username === "admin" ? "Administrator" : "Support Agent"}`}
          </button>
        </form>
      </div>
    </div>
  );
}

function DashboardView({ departments = DEFAULT_DEPARTMENTS, authUser, onSignOut }) {
  const depts = departments && departments.length > 0 ? departments : DEFAULT_DEPARTMENTS;
  const credentials = authUser.credentials;
  const [tickets, setTickets] = useState([]);
  const [summary, setSummary] = useState(null);
  const [filters, setFilters] = useState({ departmentId: "", status: "", priority: "" });
  const [drafts, setDrafts] = useState({});
  const [activeTimeline, setActiveTimeline] = useState(null);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [loading, setLoading] = useState(false);

  const isAdmin = authUser.role === "ADMIN";

  async function loadData(activeFilters = filters) {
    setLoading(true);
    setError("");
    try {
      const [ticketData, summaryData] = await Promise.all([
        getTickets(credentials, activeFilters),
        getDashboardSummary(credentials)
      ]);
      setTickets(ticketData);
      setSummary(summaryData);
      setDrafts({});
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData(filters);
  }, [filters]);

  function updateFilter(key, value) {
    setFilters((prev) => ({ ...prev, [key]: value }));
  }

  function changeDraft(ticket, field, value) {
    setDrafts((prev) => ({
      ...prev,
      [ticket.id]: {
        status: field === "status" ? value : (prev[ticket.id]?.status ?? ticket.status),
        adminNote: field === "adminNote" ? value : (prev[ticket.id]?.adminNote ?? ticket.adminNote ?? "")
      }
    }));
  }

  async function saveTicket(ticket) {
    const draft = drafts[ticket.id];
    if (!draft) return;
    try {
      await updateTicket(ticket.id, draft, credentials);
      setSuccessMsg(`Ticket ${ticket.ticketNumber} updated successfully.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function removeTicket(ticket) {
    if (!isAdmin) {
      alert("Only administrators have permission to delete tickets.");
      return;
    }
    if (!window.confirm(`Delete ${ticket.ticketNumber}? This will cascade delete its audit records.`)) return;
    try {
      await deleteTicket(ticket.id, credentials);
      setSuccessMsg(`Ticket ${ticket.ticketNumber} removed.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function showAuditTimeline(ticketId) {
    try {
      const detail = await getTicketDetails(ticketId, credentials);
      setActiveTimeline(detail);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  const cards = summary ? [
    ["Total Tickets", summary.totalTickets],
    ["Open Queue", summary.openTickets],
    ["In Progress", summary.inProgressTickets],
    ["Resolved", summary.resolvedTickets],
    ["High Priority", summary.highPriorityTickets]
  ] : [];

  return (
    <section className="admin-section">
      <div className="admin-heading">
        <div>
          <p className="eyebrow">Support Staff</p>
          <h2>Support Dashboard</h2>
        </div>
        <div className="user-badge-group">
          <span className="user-role-badge">
            Signed in as <strong>{authUser.username}</strong> ({authUser.role})
          </span>
          <button className="secondary-button small" onClick={onSignOut}>Sign out</button>
        </div>
      </div>

      {error && <p className="message error">{error}</p>}
      {successMsg && <p className="message success">{successMsg}</p>}

      <div className="summary-grid">
        {cards.map(([label, value]) => (
          <div className="summary-card" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>

      {summary?.ticketsByDepartment && (
        <div className="card department-workload">
          <h4>Tickets by Department</h4>
          <div className="dept-tags">
            {Object.entries(summary.ticketsByDepartment).map(([name, count]) => (
              <span key={name} className="dept-tag">
                <strong>{name}:</strong> {count}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="card filters">
        <label>
          Department
          <select value={filters.departmentId} onChange={(e) => updateFilter("departmentId", e.target.value)}>
            <option value="">All Departments</option>
            {depts.map((dept) => (
              <option key={dept.id} value={dept.id}>{dept.name}</option>
            ))}
          </select>
        </label>

        <label>
          Status
          <select value={filters.status} onChange={(event) => updateFilter("status", event.target.value)}>
            <option value="">All Statuses</option>
            {statuses.map((status) => <option key={status} value={status}>{readable(status)}</option>)}
          </select>
        </label>

        <label>
          Priority
          <select value={filters.priority} onChange={(event) => updateFilter("priority", event.target.value)}>
            <option value="">All Priorities</option>
            {priorities.map((priority) => <option key={priority} value={priority}>{readable(priority)}</option>)}
          </select>
        </label>

        <button className="secondary-button" onClick={() => loadData()}>Refresh</button>
      </div>

      <div className="ticket-list">
        {loading && <p className="muted">Loading tickets from database...</p>}
        {!loading && tickets.length === 0 && <p className="empty-state">No tickets found matching these filters.</p>}

        {tickets.map((ticket) => {
          const draft = drafts[ticket.id] ?? { status: ticket.status, adminNote: ticket.adminNote ?? "" };
          return (
            <article className="ticket-card" key={ticket.id}>
              <div className="ticket-main">
                <div className="ticket-meta">
                  <strong>{ticket.ticketNumber}</strong>
                  <span className={`badge ${ticket.priority.toLowerCase()}`}>{readable(ticket.priority)}</span>
                  <span className="badge neutral">{ticket.departmentName}</span>
                  <span className={`badge status-${ticket.status.toLowerCase()}`}>{readable(ticket.status)}</span>
                </div>
                <h3>{ticket.title}</h3>
                <p className="muted">
                  Requester: {ticket.requesterName} ({ticket.requesterEmail}) · {formatDate(ticket.createdAt)}
                </p>
                <p>{ticket.description}</p>
                <button className="text-link" onClick={() => showAuditTimeline(ticket.id)}>
                  View Ticket History
                </button>
              </div>

              <div className="ticket-actions">
                <label>
                  Change Status
                  <select value={draft.status} onChange={(event) => changeDraft(ticket, "status", event.target.value)}>
                    {statuses.map((status) => <option key={status} value={status}>{readable(status)}</option>)}
                  </select>
                </label>

                <label>
                  Resolution Note
                  <textarea
                    value={draft.adminNote}
                    onChange={(event) => changeDraft(ticket, "adminNote", event.target.value)}
                    maxLength="500"
                    placeholder="Log resolution actions or update requester..."
                  />
                </label>

                <div className="action-row">
                  <button className="primary-button small" onClick={() => saveTicket(ticket)}>Update Ticket</button>
                  {isAdmin && (
                    <button className="danger-button" onClick={() => removeTicket(ticket)}>Delete</button>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>

      {activeTimeline && (
        <div className="modal-overlay" onClick={() => setActiveTimeline(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Ticket History: {activeTimeline.ticket.ticketNumber}</h3>
              <button className="close-btn" onClick={() => setActiveTimeline(null)}>&times;</button>
            </div>
            <p className="muted">{activeTimeline.ticket.title} ({activeTimeline.ticket.departmentName})</p>
            <div className="timeline" style={{ marginTop: "16px" }}>
              {activeTimeline.timeline.map((log) => (
                <div key={log.id} className="timeline-item">
                  <div className="timeline-marker" />
                  <div className="timeline-content">
                    <div className="timeline-title">
                      <strong>{readable(log.action)}</strong>
                      <small className="muted">{formatDate(log.createdAt)}</small>
                    </div>
                    <p className="muted">
                      {log.oldStatus ? `${readable(log.oldStatus)} → ` : ""}
                      <span className="highlight-status">{readable(log.newStatus)}</span>
                      {log.changedByName && ` · by ${log.changedByName}`}
                    </p>
                    {log.note && <p className="timeline-note">{log.note}</p>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

export default function App() {
  const [view, setView] = useState("submit");
  const [departments, setDepartments] = useState(DEFAULT_DEPARTMENTS);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [authUser, setAuthUser] = useState(null);
  const [apiStatus, setApiStatus] = useState("connecting");

  useEffect(() => {
    let isMounted = true;
    function ping() {
      checkHealth()
        .then(() => {
          if (isMounted) setApiStatus("connected");
        })
        .catch(() => {
          if (isMounted) setApiStatus("offline");
        });
    }
    ping();
    const interval = setInterval(ping, 20000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  useEffect(() => {
    getDepartments()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          setDepartments(data);
        }
      })
      .catch((err) => console.warn("Using default departments fallback:", err?.message || err));
  }, []);

  function handleLoginSuccess(user) {
    setAuthUser(user);
    setView("dashboard");
  }

  function handleSignOut() {
    setAuthUser(null);
    if (view === "dashboard") {
      setView("submit");
    }
  }

  return (
    <main>
      <header className="topbar">
        <div className="brand">
          <span>S</span>
          <div>
            <strong>SupportDesk</strong>
            <small>Campus IT & Student Support Desk</small>
          </div>
        </div>

        <nav>
          <button
            className={view === "submit" ? "nav-button active" : "nav-button"}
            onClick={() => setView("submit")}
          >
            Raise a Ticket
          </button>
          <button
            className={view === "track" ? "nav-button active" : "nav-button"}
            onClick={() => setView("track")}
          >
            Track Ticket
          </button>
          {authUser && (
            <button
              className={view === "dashboard" ? "nav-button active" : "nav-button"}
              onClick={() => setView("dashboard")}
            >
              Support Dashboard <span className="nav-role-tag">{authUser.role}</span>
            </button>
          )}
        </nav>

        <div className="topbar-actions">
          <div className="connection-badge" title="Backend connection status">
            <span className={`status-dot ${apiStatus}`} />
            <span>
              {apiStatus === "connected" && "Online"}
              {apiStatus === "connecting" && "Connecting..."}
              {apiStatus === "offline" && "Reconnecting..."}
            </span>
          </div>

          {authUser ? (
            <div className="auth-status">
              <span className="user-name">{authUser.username}</span>
              <button className="secondary-button small" onClick={handleSignOut}>Sign Out</button>
            </div>
          ) : (
            <button className="secondary-button" onClick={() => setShowLoginModal(true)}>
              Sign In
            </button>
          )}
        </div>
      </header>

      <div className="page-container">
        {view === "submit" && <TicketForm departments={departments} />}
        {view === "track" && <TrackTicketTab />}
        {view === "dashboard" && authUser && (
          <DashboardView
            departments={departments}
            authUser={authUser}
            onSignOut={handleSignOut}
          />
        )}
      </div>

      {showLoginModal && (
        <LoginModal
          onClose={() => setShowLoginModal(false)}
          onLoginSuccess={handleLoginSuccess}
        />
      )}
    </main>
  );
}
