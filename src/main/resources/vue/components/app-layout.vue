<template id="app-layout">
  <div class="app-layout">
    <div class="container">
      <!-- Start of navbar -->
      <nav class="navbar navbar-expand-lg navbar-dark mordor-navbar">
        <a class="navbar-brand" href="/">Mordor Trek</a>
        <button
          class="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="navbarSupportedContent">
          <ul class="navbar-nav me-auto">
            <li class="nav-item active">
              <a class="nav-link" href="/users">Companions</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="/activities">Journeys</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="/milestones">Milestones</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="/achievements">Achievements</a>
            </li>
          </ul>
        </div>
      </nav>
      <!--End of nav bar-->
      <!--Start of main content area-->
      <div class="content mt-3">
        <div class="container-fluid">
          <slot></slot>
        </div>
      </div>
      <!--End of main content area-->
    </div>
  </div>
</template>

<script>
app.component("app-layout", { template: "#app-layout" });
</script>

<style>
body {
  background: radial-gradient(
    circle at top,
    #3b1f2b 0,
    #050608 55%,
    #000000 100%
  );
  background-repeat: no-repeat;
  background-attachment: fixed;
  background-size: cover;
  color: #f8f9fa;
}

.mordor-navbar {
  background: linear-gradient(90deg, #3b1f2b, #8b0000);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.6);
}

.navbar-brand {
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-layout .card {
  background: rgba(20, 20, 24, 0.95);
  border: 1px solid rgba(255, 140, 0, 0.3);
  color: #f8f9fa;
}

.app-layout .card.bg-light,
.app-layout .card .card-body {
  background-color: #15151a !important;
  color: #f8f9fa;
}

.app-layout .card-header {
  background-color: #2b1b1b;
  background-image: linear-gradient(90deg, #2b1b1b, #5a1a1a);
  border-bottom: 1px solid rgba(255, 140, 0, 0.5);
  font-weight: 600;
  color: #f8f9fa;
}

.app-layout .btn-primary {
  background: linear-gradient(90deg, #e67e22, #c0392b);
  border: none;
}

.app-layout .btn-primary:hover {
  background: linear-gradient(90deg, #ff9f43, #e74c3c);
}

.mordor-progress .progress {
  background-color: #241616;
}

.mordor-progress .progress-bar {
  background: linear-gradient(90deg, #f39c12, #e74c3c);
}

.text-muted {
  color: #f8f9fa !important;
  opacity: 0.85;
}

.app-layout .form-control,
.app-layout .input-group-text {
  background-color: #15151a;
  border-color: #3a3a44;
  color: #f8f9fa;
}

.app-layout .form-control::placeholder {
  color: #9a9aa5;
}

.app-layout .table {
  background-color: #17171c;
  color: #f8f9fa;
}

.app-layout .table thead,
.app-layout .table thead th,
.app-layout .table tbody tr,
.app-layout .table tbody td {
  background-color: #111116;
  color: #f8f9fa;
}

.app-layout .table td,
.app-layout .table th {
  border-color: #33333d;
}

.app-layout .table-striped tbody tr:nth-of-type(odd) {
  background-color: #15151a;
}

.app-layout .table-striped tbody tr:nth-of-type(even) {
  background-color: #101015;
}

.app-layout .list-group-item {
  background-color: #15151a;
  color: #f8f9fa;
  border-color: #33333d;
}

.app-layout .btn-info {
  background: linear-gradient(90deg, #636e72, #2d3436);
  border: none;
  color: #f8f9fa;
}

.app-layout .btn-info:hover {
  background: linear-gradient(90deg, #b2bec3, #636e72);
}

.mordor-map {
  height: 320px;
  border: 1px solid rgba(255, 140, 0, 0.4);
  border-radius: 6px;
}
</style>
