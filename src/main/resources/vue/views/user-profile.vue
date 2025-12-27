<template id="user-profile">
  <app-layout>
    <div v-if="noUserFound">
      <p> We're sorry, we were not able to retrieve this user.</p>
      <p> View <a :href="'/users'">all users</a>.</p>
    </div>
    <div class="card bg-light mb-3" v-if="!noUserFound">
      <div class="card-header">
        <div class="row">
          <div class="col-6"> Companion Profile </div>
          <div class="col" align="right">
            <button rel="tooltip" title="Update"
                    class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin"
                    @click="updateUser()">
              <i class="far fa-save" aria-hidden="true"></i>
            </button>
            <button rel="tooltip" title="Delete"
                    class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin"
                    @click="deleteUser()">
              <i class="fas fa-trash" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="card-body">
        <form>
          <div class="input-group mb-3">
            <div class="input-group-prepend">
              <span class="input-group-text" id="input-user-id">User ID</span>
            </div>
            <input type="number" class="form-control" v-model="user.id" name="id" readonly placeholder="Id"/>
          </div>
          <div class="input-group mb-3">
            <div class="input-group-prepend">
              <span class="input-group-text" id="input-user-name">Name</span>
            </div>
            <input type="text" class="form-control" v-model="user.name" name="name" placeholder="Name"/>
          </div>
          <div class="input-group mb-3">
            <div class="input-group-prepend">
              <span class="input-group-text" id="input-user-email">Email</span>
            </div>
            <input type="email" class="form-control" v-model="user.email" name="email" placeholder="Email"/>
          </div>
        </form>

        <hr />
        <h5>Log steps towards Mordor</h5>
        <div class="input-group mb-3">
          <div class="input-group-prepend">
            <span class="input-group-text" id="input-steps">Steps</span>
          </div>
          <input type="number" min="1" class="form-control" v-model.number="newSteps" name="steps" placeholder="Enter number of steps"/>
        </div>
        <div class="input-group mb-3">
          <div class="input-group-prepend">
            <span class="input-group-text" id="input-steps-description">Description</span>
          </div>
          <input type="text" class="form-control" v-model="newStepsDescription" name="stepsDescription" placeholder="e.g. Walked from the Shire to Bree"/>
        </div>
        <button class="btn btn-primary" @click="logSteps()">Log Steps</button>

        <hr />
        <h5>Map a journey</h5>
        <p class="text-muted mb-2">
          Click the map to place a start pin, then an end pin. Drag pins to refine the route.
        </p>
        <div id="activity-map" class="mordor-map mb-2"></div>
        <div class="d-flex align-items-center mb-3">
          <span class="me-3">
            Distance: <strong>{{ canLogMapJourney ? mapDistanceKm.toFixed(2) : "-" }} km</strong>
          </span>
          <button class="btn btn-primary me-2" :disabled="!canLogMapJourney || mapBusy" @click="logMapJourney()">
            {{ mapBusy ? "Saving..." : "Log Map Journey" }}
          </button>
          <button class="btn btn-secondary" :disabled="!startPoint && !endPoint" @click="clearMapPins()">
            Clear Pins
          </button>
        </div>
      </div>
      <div class="card-footer text-left">
        <p  v-if="activities.length === 0"> No journeys yet...</p>
        <p  v-if="activities.length > 0"> Journeys so far...</p>
        <div v-if="totalSteps > 0" class="mb-2 mordor-progress">
          <p class="mb-1"><strong>Total steps taken:</strong> {{ totalSteps }}</p>
          <div class="progress">
            <div class="progress-bar" role="progressbar"
                 :style="{ width: mordorProgressPercent + '%' }"
                 :aria-valuenow="mordorProgressPercent" aria-valuemin="0" aria-valuemax="100">
              {{ mordorProgressPercent }}%
            </div>
          </div>
          <small class="text-muted">Goal: 1,800,000 steps to reach Mordor</small>
        </div>
        <ul>
          <li v-for="(activity, index) in activities" :key="activity.id">
            {{ activity.description }} for {{ activity.duration }} minutes
            <span v-if="activity.steps && activity.steps > 0">
              ({{ activity.steps }} steps)
            </span>
            <span v-if="activity.distanceKm && activity.distanceKm > 0">
              - {{ activity.distanceKm.toFixed(2) }} km
            </span>
            <button rel="tooltip" title="Delete Journey" class="btn btn-info btn-simple btn-link btn-sm ms-2"
                    :disabled="!isAdmin"
                    @click="deleteActivity(activity, index)">
              <i class="fas fa-trash" aria-hidden="true"></i>
            </button>
          </li>
        </ul>
      </div>
    </div>
  </app-layout>
</template>

<script>
app.component("user-profile", {
  template: "#user-profile",
  data: () => ({
    user: {
      id: null,
      name: "",
      email: ""
    },
    noUserFound: false,
    activities: [],
    newSteps: null,
    newStepsDescription: "",
    map: null,
    startMarker: null,
    endMarker: null,
    startPoint: null,
    endPoint: null,
    mapDistanceKm: 0,
    mapBusy: false,
    role: "user"
  }),
  created: function () {
    this.loadRole();
    const userId = this.$javalin.pathParams["user-id"];
    const url = `/api/users/${userId}`
    axios.get(url)
        .then(res => this.user = res.data)
        .catch(error => {
          console.log("No user found for id passed in the path parameter: " + error)
          this.noUserFound = true
        })
    axios.get(url + `/activities`)
        .then(res => this.activities = res.data)
        .catch(error => {
          console.log("No activities added yet (this is ok): " + error)
        })
  },
  mounted: function () {
    this.$nextTick(() => {
      this.initMap();
    });
  },
  computed: {
    totalSteps() {
      return this.activities.reduce((sum, a) => sum + (a.steps || 0), 0);
    },
    mordorProgressPercent() {
      const goal = 1800000;
      if (!goal) return 0;
      const pct = Math.round((this.totalSteps / goal) * 100);
      return Math.min(100, pct);
    },
    canLogMapJourney() {
      return this.startPoint && this.endPoint;
    },
    isAdmin() {
      return (this.role || "").toLowerCase() === "admin";
    }
  },
  methods: {
    loadRole() {
      try {
        const storedRole = window.localStorage ? window.localStorage.getItem("mordorRole") : null;
        if (storedRole) {
          this.role = storedRole;
        }
      } catch (error) {
        console.log("Unable to load saved role", error);
      }
    },
    initMap: function () {
      if (!window.L) {
        console.log("Leaflet is not available; map will not be initialised.");
        return;
      }
      if (this.map) {
        return;
      }
      const container = document.getElementById("activity-map");
      if (!container) {
        return;
      }
      this.map = L.map(container, { scrollWheelZoom: false })
          .setView([53.3498, -6.2603], 12);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(this.map);

      this.map.on("click", this.handleMapClick);

      setTimeout(() => {
        if (this.map) {
          this.map.invalidateSize();
        }
      }, 0);
    },
    handleMapClick: function (event) {
      if (!this.map) {
        return;
      }
      if (this.startMarker && this.endMarker) {
        this.clearMapPins();
      }

      if (!this.startMarker) {
        this.startMarker = L.marker(event.latlng, { draggable: true })
            .addTo(this.map)
            .bindPopup("Start")
            .openPopup();
        this.startPoint = event.latlng;
        this.startMarker.on("dragend", () => {
          this.startPoint = this.startMarker.getLatLng();
          this.updateMapDistance();
        });
      } else if (!this.endMarker) {
        this.endMarker = L.marker(event.latlng, { draggable: true })
            .addTo(this.map)
            .bindPopup("End")
            .openPopup();
        this.endPoint = event.latlng;
        this.endMarker.on("dragend", () => {
          this.endPoint = this.endMarker.getLatLng();
          this.updateMapDistance();
        });
      }

      this.updateMapDistance();
    },
    updateMapDistance: function () {
      if (!this.map || !this.startPoint || !this.endPoint) {
        this.mapDistanceKm = 0;
        return;
      }
      const meters = this.map.distance(this.startPoint, this.endPoint);
      this.mapDistanceKm = meters / 1000.0;
    },
    clearMapPins: function () {
      if (this.map && this.startMarker) {
        this.map.removeLayer(this.startMarker);
      }
      if (this.map && this.endMarker) {
        this.map.removeLayer(this.endMarker);
      }
      this.startMarker = null;
      this.endMarker = null;
      this.startPoint = null;
      this.endPoint = null;
      this.mapDistanceKm = 0;
    },
    logMapJourney: function () {
      if (!this.canLogMapJourney) {
        return;
      }
      const userId = this.$javalin.pathParams["user-id"];
      this.mapBusy = true;
      axios.post(`/api/users/${userId}/activities/map`, {
        startLat: this.startPoint.lat,
        startLng: this.startPoint.lng,
        endLat: this.endPoint.lat,
        endLng: this.endPoint.lng
      })
          .then(response => {
            this.activities.push(response.data);
            this.clearMapPins();
          })
          .catch(error => {
            console.log(error);
            alert("Error logging map journey");
          })
          .finally(() => {
            this.mapBusy = false;
          });
    },
    updateUser: function () {
      if (!this.isAdmin) {
        alert("Admin role required to update users.");
        return;
      }
      const userId = this.$javalin.pathParams["user-id"];
      const url = `/api/users/${userId}`
      const headers = { "X-User-Role": this.role };
      axios.patch(url,
          {
            name: this.user.name,
            email: this.user.email
          },
          { headers })
          .then(() => {
            alert("User updated!")
          })
          .catch(error => {
            console.log(error)
            alert("Error updating user")
          })
    },
    deleteUser: function () {
      if (!this.isAdmin) {
        alert("Admin role required to delete users.");
        return;
      }
      if (confirm("Do you really want to delete?")) {
        const userId = this.$javalin.pathParams["user-id"];
        const url = `/api/users/${userId}`
        const headers = { "X-User-Role": this.role };
        axios.delete(url, { headers })
            .then(response => {
              alert("User deleted")
              //display the /users endpoint
              window.location.href = '/users';
            })
            .catch(function (error) {
              console.log(error)
            });
      }
    },
    deleteActivity: function (activity, index) {
      if (!this.isAdmin) {
        alert("Admin role required to delete activities.");
        return;
      }
      if (!confirm("Do you really want to delete this journey?")) {
        return;
      }
      const url = `/api/activities/${activity.id}`;
      const headers = { "X-User-Role": this.role };
      axios.delete(url, { headers })
          .then(() => {
            this.activities.splice(index, 1);
          })
          .catch(error => {
            console.log(error);
            alert("Error deleting journey");
          });
    },
    logSteps: function () {
      const userId = this.$javalin.pathParams["user-id"];
      const steps = this.newSteps;
      if (!steps || steps <= 0) {
        alert("Please enter a positive number of steps");
        return;
      }

      const durationMinutes = steps / 100.0;
      const calories = Math.round(steps * 0.04);
      const description = this.newStepsDescription || `Walked ${steps} steps towards Mordor`;

      axios.post("/api/activities", {
        description: description,
        duration: durationMinutes,
        calories: calories,
        started: new Date().toISOString(),
        userId: parseInt(userId),
        steps: steps
      })
          .then(response => {
            this.activities.push(response.data);
            this.newSteps = null;
            this.newStepsDescription = "";
          })
          .catch(error => {
            console.log(error);
            alert("Error logging steps");
          });
    }
  }

});
</script>
