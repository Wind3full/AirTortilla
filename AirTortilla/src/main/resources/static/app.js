const $ = (selector) => document.querySelector(selector);

async function api(path, options = {}) {
  const response = await fetch(path, {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    let message = `Error ${response.status}`;

    try {
      const data = await response.json();
      message = data.detail || data.message || data.error || message;
    } catch {
      // La respuesta de error no siempre contiene JSON.
    }

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return response;
}

function escapeHtml(value) {
  const replacements = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  };

  return String(value).replace(/[&<>"']/g, character => replacements[character]);
}

function showMessage(selector, message) {
  const element = $(selector);
  if (element) {
    element.textContent = message;
  }
}

function renderHeader(user) {
  const header = $('#app-header');
  const isAdministrator = user.role === 'ADMINISTRADOR';
  const adminLink = isAdministrator
    ? '<a href="/admin.html">Administración</a>'
    : '';

  header.innerHTML = `
    <header class="app-header">
      <a class="brand-link" href="/inicio.html">
        <strong>AirTortilla</strong>
        <span>Gestión de reservas</span>
      </a>

      <nav class="main-nav">
        <a href="/inicio.html">Inicio</a>
        <a href="/reservas.html">Ver registros</a>
        <a href="/consultar.html">Consultar destino</a>
        <a href="/csv.html">Generar CSV</a>
        <a href="/nueva-reserva.html">Añadir registro</a>
        ${adminLink}
      </nav>

      <div class="user-menu">
        <span>
          ${escapeHtml(user.username)} ·
          ${isAdministrator ? 'Administrador' : 'Empleado'}
        </span>
        <button id="logout" class="secondary small-button">Salir</button>
      </div>
    </header>
  `;

  const currentPage = location.pathname;
  header.querySelectorAll('.main-nav a').forEach(link => {
    if (link.getAttribute('href') === currentPage) {
      link.classList.add('active');
    }
  });

  $('#logout').addEventListener('click', async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } finally {
      location.assign('/');
    }
  });
}

async function loadReservations(destination) {
  const query = destination
    ? `?destino=${encodeURIComponent(destination)}`
    : '';
  const response = await api(`/api/reservas${query}`);
  const reservations = await response.json();

  $('#reservations').innerHTML = reservations
    .map(reservation => `
      <tr>
        <td>${escapeHtml(reservation.localizador)}</td>
        <td>${escapeHtml(reservation.nombrePasajero)}</td>
        <td>${escapeHtml(reservation.origen)}</td>
        <td>${escapeHtml(reservation.destino)}</td>
        <td>${escapeHtml(reservation.fechaReserva)}</td>
      </tr>
    `)
    .join('');

  showMessage(
    '#page-message',
    `${reservations.length} reserva(s) encontrada(s).`
  );
}

async function downloadCsv() {
  const response = await api('/api/reservas/csv');
  const csvZip = await response.blob();
  const downloadUrl = URL.createObjectURL(csvZip);
  const link = document.createElement('a');

  link.href = downloadUrl;
  link.download = 'AirTortilla_CSV.zip';
  link.click();
  URL.revokeObjectURL(downloadUrl);

  showMessage('#page-message', 'Descarga iniciada.');
}

async function loadAudit() {
  const response = await api('/api/audit');
  const entries = await response.json();

  $('#audit-list').innerHTML = entries
    .map(entry => `
      <li>
        ${escapeHtml(entry.fecha)} ·
        ${escapeHtml(entry.username)} ·
        ${escapeHtml(entry.accion)} ·
        ${escapeHtml(entry.registroAfectado)}
      </li>
    `)
    .join('');
}

function connectPage() {
  const reloadButton = $('#reload-reservations');
  reloadButton?.addEventListener('click', () => {
    loadReservations().catch(error => {
      showMessage('#page-message', error.message);
    });
  });

  if ($('#reservations') && location.pathname === '/reservas.html') {
    loadReservations().catch(error => {
      showMessage('#page-message', error.message);
    });
  }

  $('#filter-form')?.addEventListener('submit', event => {
    event.preventDefault();

    const form = event.currentTarget;
    const destination = new FormData(form).get('destino');

    loadReservations(destination).catch(error => {
      showMessage('#page-message', error.message);
    });
  });

  $('#download-csv')?.addEventListener('click', () => {
    downloadCsv().catch(error => {
      showMessage('#page-message', error.message);
    });
  });

  $('#reservation-form')?.addEventListener('submit', async event => {
    event.preventDefault();

    const form = event.currentTarget;
    if (form.dataset.submitting === 'true') {
      return;
    }

    form.dataset.submitting = 'true';
    const submitButton = form.querySelector(
      'button[type="submit"], button:not([type])'
    );
    const originalButtonText = submitButton?.textContent;

    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Guardando…';
    }

    const data = Object.fromEntries(new FormData(form));

    try {
      await api('/api/reservas', {
        method: 'POST',
        body: JSON.stringify(data)
      });

      form.reset();
      showMessage('#page-message', 'Reserva guardada correctamente.');
    } catch (error) {
      const message = error.status === 409
        ? 'Ese localizador ya pertenece a otra reserva. Comprueba los datos o utiliza otro localizador.'
        : error.message;

      showMessage('#page-message', message);
    } finally {
      delete form.dataset.submitting;

      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = originalButtonText;
      }
    }
  });

  $('#user-form')?.addEventListener('submit', async event => {
    event.preventDefault();

    const form = event.currentTarget;
    if (form.dataset.submitting === 'true') {
      return;
    }

    form.dataset.submitting = 'true';
    const submitButton = form.querySelector(
      'button[type="submit"], button:not([type])'
    );
    const originalButtonText = submitButton?.textContent;

    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Creando…';
    }

    const data = Object.fromEntries(new FormData(form));

    try {
      await api('/api/users', {
        method: 'POST',
        body: JSON.stringify(data)
      });

      form.reset();
      showMessage('#user-message', 'Usuario creado correctamente.');
    } catch (error) {
      const message = error.status === 409
        ? 'Ese nombre de usuario ya está registrado. Prueba con otro.'
        : error.message;

      showMessage('#user-message', message);
    } finally {
      delete form.dataset.submitting;

      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = originalButtonText;
      }
    }
  });

  $('#load-audit')?.addEventListener('click', () => {
    loadAudit().catch(error => {
      showMessage('#user-message', error.message);
    });
  });
}

const page = document.body.dataset.page;

if (page === 'login') {
  $('#login-form').addEventListener('submit', async event => {
    event.preventDefault();

    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));

    try {
      await api('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(data)
      });

      location.assign('/inicio.html');
    } catch (error) {
      showMessage('#login-message', error.message);
    }
  });

  api('/api/auth/me')
    .then(() => location.replace('/inicio.html'))
    .catch(() => {});
} else {
  document.body.classList.add('auth-pending');

  api('/api/auth/me')
    .then(async response => {
      const user = await response.json();

      if (
        location.pathname === '/admin.html'
        && user.role !== 'ADMINISTRADOR'
      ) {
        location.replace('/inicio.html');
        return;
      }

      renderHeader(user);

      document.querySelectorAll('.admin-only').forEach(element => {
        element.hidden = user.role !== 'ADMINISTRADOR';
      });

      document.body.classList.remove('auth-pending');
      connectPage();
    })
    .catch(() => location.replace('/'));
}
