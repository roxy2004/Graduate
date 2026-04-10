import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {path:'/',redirect:'/login'},
    {
      path:'/manager',
      component:()=>import('@/views/Manager.vue'),
      redirect:'/manager/student/dashboard',
      children:[
        {path:'teacher/users',component:()=>import('@/views/manager/TeacherUsers.vue')},
        {path:'teacher/questions',component:()=>import('@/views/manager/TeacherQuestions.vue')},
        {path:'student/dashboard',component:()=>import('@/views/manager/student/Dashboard.vue')},
        {path:'student/learning',component:()=>import('@/views/manager/student/Learning.vue')},
        {path:'student/practice',component:()=>import('@/views/manager/student/Practice.vue')},
        {path:'student/mistakes',component:()=>import('@/views/manager/student/Mistakes.vue')},
        {path:'student/profile',component:()=>import('@/views/manager/student/Profile.vue')},
        {path:'student/recommendations',component:()=>import('@/views/manager/student/Recommendations.vue')},
        {path:'student/account',component:()=>import('@/views/manager/student/Account.vue')},
        {path:'student/records',component:()=>import('@/views/manager/student/SolvedRecords.vue')},
      ]
    },
    {path:'/login',component:()=>import('@/views/Login.vue')},
  ]
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.path === '/login') {
    next()
    return
  }
  if (!user) {
    next('/login')
    return
  }
  if (to.path.startsWith('/manager/teacher') && user.role !== 'teacher') {
    next('/manager/student/dashboard')
    return
  }
  if (to.path.startsWith('/manager/student') && user.role !== 'student') {
    next('/manager/teacher/users')
    return
  }
  next()
})

export default router