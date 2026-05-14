const $ = (id) => document.getElementById(id);

const appContext = window.location.pathname
  .replace(/\/index\.html$/, "")
  .replace(/\/$/, "");

const API_BASE = `${appContext}/api`;

const state = {
  token: localStorage.getItem("ev_token"),
  username: localStorage.getItem("ev_username"),
  role: localStorage.getItem("ev_role"),
  stations: [],
  selectedStation: null,
  selectedConnector: null,
  map: null,
  markers: []
};

document.addEventListener("DOMContentLoaded", init);

function init() {
  $("loginForm").addEventListener("submit", login);
  $("logoutButton").addEventListener("click", logout);

  $("bookingDate").value = todayIso();
  $("refreshAvailabilityButton").addEventListener("click", loadAvailability);
  $("createBookingButton").addEventListener("click", createBooking);
  $("reloadBookingsButton").addEventListener("click", loadBookings);

  $("adminCreateStation").addEventListener("click", createStation);
  $("adminUpdateStation").addEventListener("click", updateSelectedStation);
  $("adminDeleteStation").addEventListener("click", deleteSelectedStation);
  $("adminCreateConnector").addEventListener("click", createConnector);
  $("adminUpdateConnector").addEventListener("click", updateConnector);
  $("adminDeleteConnector").addEventListener("click", deleteConnector);

  if (state.token) {
    showApp();
  } else {
    showLogin();
  }
}

async function login(event) {
  event.preventDefault();
  setMessage("loginMessage", "");

  try {
    const response = await api("/auth/login", {
      method: "POST",
      auth: false,
      body: {
        username: $("username").value.trim(),
        password: $("password").value
      }
    });

    state.token = response.token;
    state.username = response.username;
    state.role = response.role;

    localStorage.setItem("ev_token", state.token);
    localStorage.setItem("ev_username", state.username);
    localStorage.setItem("ev_role", state.role);

    $("password").value = "";
    showApp();
  } catch (error) {
    setMessage("loginMessage", error.message, true);
  }
}

function logout() {
  localStorage.removeItem("ev_token");
  localStorage.removeItem("ev_username");
  localStorage.removeItem("ev_role");

  state.token = null;
  state.username = null;
  state.role = null;
  state.selectedStation = null;
  state.selectedConnector = null;

  showLogin();
}

function showLogin() {
  $("loginSection").classList.remove("hidden");
  $("app").classList.add("hidden");
  $("sessionBox").classList.add("hidden");
}

function showApp() {
  $("loginSection").classList.add("hidden");
  $("app").classList.remove("hidden");
  $("sessionBox").classList.remove("hidden");

  $("userInfo").textContent = `${state.username} (${state.role})`;
  $("adminPanel").classList.toggle("hidden", state.role !== "ADMIN");

  initMap();
  loadStations();
  loadBookings();
}

async function api(path, options = {}) {
  const headers = {};

  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (options.auth !== false && state.token) {
    headers["Authorization"] = `Bearer ${state.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method || "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof data === "object" && data !== null
        ? data.error || data.message || `Request failed with ${response.status}`
        : data || `Request failed with ${response.status}`;

    if (response.status === 401) {
      logout();
    }

    throw new Error(message);
  }

  return data;
}

function initMap() {
  if (state.map) {
    setTimeout(() => state.map.invalidateSize(), 100);
    return;
  }

  state.map = L.map("map").setView([40.6401, 22.9444], 13);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(state.map);
}

async function loadStations() {
  try {
    state.stations = await api("/stations");
    renderStations();
    renderMapMarkers();
  } catch (error) {
    $("stationsList").innerHTML = `<p class="message error">${escapeHtml(error.message)}</p>`;
  }
}

function renderStations() {
  if (!state.stations || state.stations.length === 0) {
    $("stationsList").innerHTML = `<p class="muted">No stations found.</p>`;
    return;
  }

  $("stationsList").innerHTML = state.stations.map((station) => {
    const id = getStationId(station);
    return `
      <div class="list-item" onclick="selectStation(${id})">
        <strong>${escapeHtml(getStationName(station))}</strong><br>
        <span class="muted">${escapeHtml(station.address || "")}</span><br>
        <small>ID: ${id}</small>
      </div>
    `;
  }).join("");
}

function renderMapMarkers() {
  state.markers.forEach((marker) => marker.remove());
  state.markers = [];

  const bounds = [];

  state.stations.forEach((station) => {
    const lat = Number(station.latitude);
    const lng = Number(station.longitude);
    const id = getStationId(station);

    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      return;
    }

    const marker = L.marker([lat, lng])
      .addTo(state.map)
      .bindPopup(`<strong>${escapeHtml(getStationName(station))}</strong><br>${escapeHtml(station.address || "")}`)
      .on("click", () => selectStation(id));

    state.markers.push(marker);
    bounds.push([lat, lng]);
  });

  if (bounds.length > 0) {
    state.map.fitBounds(bounds, { padding: [30, 30] });
  }
}

async function selectStation(stationId) {
  try {
    const details = await api(`/stations/${stationId}`);
    state.selectedStation = details.station || details;
    state.selectedStation.connectors = details.connectors || [];
    state.selectedConnector = null;

    fillAdminStationFields();
    renderStationDetails();
    $("availabilityList").innerHTML = "";
    $("selectedConnectorText").textContent = "No connector selected.";
  } catch (error) {
    $("stationDetails").innerHTML = `<p class="message error">${escapeHtml(error.message)}</p>`;
  }
}

function renderStationDetails() {
  const station = state.selectedStation;
  const connectors = station.connectors || [];

  $("stationDetails").innerHTML = `
    <h3>${escapeHtml(getStationName(station))}</h3>
    <p>${escapeHtml(station.address || "")}</p>
    <p class="muted">
      Station ID: ${getStationId(station)} |
      Lat: ${station.latitude} |
      Lng: ${station.longitude}
    </p>

    <h3>Connectors</h3>
    ${
      connectors.length === 0
        ? `<p class="muted">No connectors for this station.</p>`
        : connectors.map((connector) => `
          <div class="connector-card">
            <strong>${escapeHtml(getConnectorType(connector))}</strong><br>
            <small>Connector ID: ${getConnectorId(connector)}</small><br>
            <button class="small" onclick="selectConnector(${getConnectorId(connector)})">
              Select connector
            </button>
          </div>
        `).join("")
    }
  `;
}

function selectConnector(connectorId) {
  if (!state.selectedStation) {
    return;
  }

  const connectors = state.selectedStation.connectors || [];
  state.selectedConnector = connectors.find((c) => getConnectorId(c) === connectorId);

  if (!state.selectedConnector) {
    return;
  }

  $("selectedConnectorText").textContent =
    `Selected connector ${connectorId} (${getConnectorType(state.selectedConnector)})`;

  $("adminConnectorStationId").value = getStationId(state.selectedStation);
  $("adminConnectorId").value = getConnectorId(state.selectedConnector);
  $("adminConnectorType").value = getConnectorType(state.selectedConnector);

  loadAvailability();
}

async function loadAvailability() {
  setMessage("bookingMessage", "");

  if (!state.selectedConnector) {
    setMessage("bookingMessage", "Select a connector first.", true);
    return;
  }

  const date = $("bookingDate").value;

  if (!date) {
    setMessage("bookingMessage", "Select a date.", true);
    return;
  }

  try {
    const connectorId = getConnectorId(state.selectedConnector);
    const slots = await api(`/connectors/${connectorId}/availability?date=${encodeURIComponent(date)}`);

    if (!slots || slots.length === 0) {
      $("availabilityList").innerHTML = `<p class="muted">No available slots for this connector/date.</p>`;
      return;
    }

    $("availabilityList").innerHTML = slots.map((slot) => {
      const start = slot.start_time || slot.startTime;
      const end = slot.end_time || slot.endTime;

      return `
        <button class="slot" onclick="pickSlot('${escapeAttr(start)}', '${escapeAttr(end)}')">
          ${escapeHtml(start)} - ${escapeHtml(end)}
        </button>
      `;
    }).join("");
  } catch (error) {
    $("availabilityList").innerHTML = `<p class="message error">${escapeHtml(error.message)}</p>`;
  }
}

function pickSlot(start, end) {
  $("startTime").value = start.substring(0, 5);
  $("endTime").value = end.substring(0, 5);
}

async function createBooking() {
  setMessage("bookingMessage", "");

  if (!state.selectedConnector) {
    setMessage("bookingMessage", "Select a connector first.", true);
    return;
  }

  try {
    await api("/bookings", {
      method: "POST",
      body: {
        connector_id: getConnectorId(state.selectedConnector),
        booking_date: $("bookingDate").value,
        start_time: $("startTime").value,
        end_time: $("endTime").value
      }
    });

    setMessage("bookingMessage", "Booking created successfully.", false);
    await loadAvailability();
    await loadBookings();
  } catch (error) {
    setMessage("bookingMessage", error.message, true);
  }
}

async function loadBookings() {
  try {
    const bookings = await api("/bookings");
    renderBookings(bookings || []);
  } catch (error) {
    $("bookingsBody").innerHTML = `
      <tr><td colspan="9" class="message error">${escapeHtml(error.message)}</td></tr>
    `;
  }
}

function renderBookings(bookings) {
  if (bookings.length === 0) {
    $("bookingsBody").innerHTML = `<tr><td colspan="9" class="muted">No bookings found.</td></tr>`;
    return;
  }

  $("bookingsBody").innerHTML = bookings.map((booking) => {
    const id = booking.booking_id || booking.bookingId;
    const connectorId = booking.connector_id || booking.connectorId;
    const stationId = booking.station_id || booking.stationId;
    const bookingDate = booking.booking_date || booking.bookingDate;
    const start = booking.start_time || booking.startTime;
    const end = booking.end_time || booking.endTime;

    return `
      <tr>
        <td>${id}</td>
        <td>${escapeHtml(booking.username || "")}</td>
        <td>${stationId}</td>
        <td>${connectorId}</td>
        <td>${escapeHtml(bookingDate)}</td>
        <td>${escapeHtml(start)}</td>
        <td>${escapeHtml(end)}</td>
        <td>${escapeHtml(booking.status || "")}</td>
        <td>
          <button class="small secondary" onclick="editBooking(${id}, ${connectorId}, '${escapeAttr(bookingDate)}', '${escapeAttr(start)}', '${escapeAttr(end)}')">
            Modify
          </button>
          <button class="small danger" onclick="cancelBooking(${id})">
            Cancel
          </button>
        </td>
      </tr>
    `;
  }).join("");
}

async function editBooking(id, connectorId, date, start, end) {
  const newConnectorId = prompt("Connector ID:", connectorId);
  const newDate = prompt("Booking date YYYY-MM-DD:", date);
  const newStart = prompt("Start time HH:MM:", start.substring(0, 5));
  const newEnd = prompt("End time HH:MM:", end.substring(0, 5));

  if (!newConnectorId || !newDate || !newStart || !newEnd) {
    return;
  }

  try {
    await api(`/bookings/${id}`, {
      method: "PUT",
      body: {
        connector_id: Number(newConnectorId),
        booking_date: newDate,
        start_time: newStart,
        end_time: newEnd
      }
    });

    await loadBookings();
    if (state.selectedConnector) {
      await loadAvailability();
    }
  } catch (error) {
    alert(error.message);
  }
}

async function cancelBooking(id) {
  if (!confirm(`Cancel booking ${id}?`)) {
    return;
  }

  try {
    await api(`/bookings/${id}`, { method: "DELETE" });
    await loadBookings();

    if (state.selectedConnector) {
      await loadAvailability();
    }
  } catch (error) {
    alert(error.message);
  }
}

function fillAdminStationFields() {
  if (!state.selectedStation) {
    return;
  }

  $("adminStationName").value = getStationName(state.selectedStation);
  $("adminStationAddress").value = state.selectedStation.address || "";
  $("adminStationLat").value = state.selectedStation.latitude || "";
  $("adminStationLng").value = state.selectedStation.longitude || "";
  $("adminConnectorStationId").value = getStationId(state.selectedStation);
}

async function createStation() {
  try {
    await api("/stations", {
      method: "POST",
      body: stationPayload()
    });

    setMessage("adminMessage", "Station created.", false);
    await loadStations();
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

async function updateSelectedStation() {
  if (!state.selectedStation) {
    setMessage("adminMessage", "Select a station first.", true);
    return;
  }

  try {
    await api(`/stations/${getStationId(state.selectedStation)}`, {
      method: "PUT",
      body: stationPayload()
    });

    setMessage("adminMessage", "Station updated.", false);
    await loadStations();
    await selectStation(getStationId(state.selectedStation));
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

async function deleteSelectedStation() {
  if (!state.selectedStation) {
    setMessage("adminMessage", "Select a station first.", true);
    return;
  }

  if (!confirm("Delete selected station?")) {
    return;
  }

  try {
    await api(`/stations/${getStationId(state.selectedStation)}`, { method: "DELETE" });
    setMessage("adminMessage", "Station deleted.", false);

    state.selectedStation = null;
    state.selectedConnector = null;
    $("stationDetails").innerHTML = "Select a station from the map or list.";
    await loadStations();
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

async function createConnector() {
  const stationId = Number($("adminConnectorStationId").value);

  if (!stationId) {
    setMessage("adminMessage", "Station ID is required.", true);
    return;
  }

  try {
    await api(`/stations/${stationId}/connectors`, {
      method: "POST",
      body: {
        connector_type: $("adminConnectorType").value.trim()
      }
    });

    setMessage("adminMessage", "Connector created.", false);
    await loadStations();

    if (state.selectedStation && getStationId(state.selectedStation) === stationId) {
      await selectStation(stationId);
    }
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

async function updateConnector() {
  const connectorId = Number($("adminConnectorId").value);

  if (!connectorId) {
    setMessage("adminMessage", "Connector ID is required.", true);
    return;
  }

  try {
    await api(`/connectors/${connectorId}`, {
      method: "PUT",
      body: {
        connector_type: $("adminConnectorType").value.trim()
      }
    });

    setMessage("adminMessage", "Connector updated.", false);

    if (state.selectedStation) {
      await selectStation(getStationId(state.selectedStation));
    }
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

async function deleteConnector() {
  const connectorId = Number($("adminConnectorId").value);

  if (!connectorId) {
    setMessage("adminMessage", "Connector ID is required.", true);
    return;
  }

  if (!confirm(`Delete connector ${connectorId}?`)) {
    return;
  }

  try {
    await api(`/connectors/${connectorId}`, { method: "DELETE" });
    setMessage("adminMessage", "Connector deleted.", false);

    state.selectedConnector = null;

    if (state.selectedStation) {
      await selectStation(getStationId(state.selectedStation));
    }
  } catch (error) {
    setMessage("adminMessage", error.message, true);
  }
}

function stationPayload() {
  return {
    name: $("adminStationName").value.trim(),
    address: $("adminStationAddress").value.trim(),
    latitude: Number($("adminStationLat").value),
    longitude: Number($("adminStationLng").value)
  };
}

function getStationId(station) {
  return station.stationId ?? station.station_id;
}

function getStationName(station) {
  return station.name ?? station.stationName ?? station.station_name ?? "Unnamed station";
}

function getConnectorId(connector) {
  return connector.connectorId ?? connector.connector_id;
}

function getConnectorType(connector) {
  return connector.connectorType ?? connector.connector_type ?? "Unknown connector";
}

function todayIso() {
  return new Date().toISOString().substring(0, 10);
}

function setMessage(elementId, text, isError = false) {
  const element = $(elementId);
  element.textContent = text;
  element.className = `message ${text ? (isError ? "error" : "success") : ""}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value).replaceAll("`", "&#096;");
}