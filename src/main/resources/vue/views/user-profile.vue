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
                    @click="updateUser()">
              <i class="far fa-save" aria-hidden="true"></i>
            </button>
            <button rel="tooltip" title="Delete"
                    class="btn btn-info btn-simple btn-link"
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
            <button rel="tooltip" title="Delete Journey" class="btn btn-info btn-simple btn-link btn-sm ms-2"
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
    user: null,
    noUserFound: false,
    activities: [],
    newSteps: null,
    newStepsDescription: "",
  }),
  created: function () {
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
  computed: {
    totalSteps() {
      return this.activities.reduce((sum, a) => sum + (a.steps || 0), 0);
    },
    mordorProgressPercent() {
      const goal = 1800000;
      if (!goal) return 0;
      const pct = Math.round((this.totalSteps / goal) * 100);
      return Math.min(100, pct);
    }
  },
  methods: {
    updateUser: function () {
      const userId = this.$javalin.pathParams["user-id"];
      const url = `/api/users/${userId}`
      axios.patch(url,
          {
            name: this.user.name,
            email: this.user.email
          })
          .then(() => {
            alert("User updated!")
          })
          .catch(error => {
            console.log(error)
            alert("Error updating user")
          })
    },
    deleteUser: function () {
      if (confirm("Do you really want to delete?")) {
        const userId = this.$javalin.pathParams["user-id"];
        const url = `/api/users/${userId}`
        axios.delete(url)
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
      if (!confirm("Do you really want to delete this journey?")) {
        return;
      }
      const url = `/api/activities/${activity.id}`;
      axios.delete(url)
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
