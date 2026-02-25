<script setup lang="ts">
import { RouterView, useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/admin/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">餐厅管理系统</div>
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="handleSelect"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/tables">
          <el-icon><TakeawayBox /></el-icon>
          <span>桌台管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/dishes">
          <el-icon><Food /></el-icon>
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/kitchen">
          <el-icon><Dish /></el-icon>
          <span>后厨管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/reports">
          <el-icon><TrendCharts /></el-icon>
          <span>统计报表</span>
        </el-menu-item>
      </el-menu>
      
      <div class="logout" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </div>
    </el-aside>
    
    <el-main class="main-content">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.admin-menu {
  flex: 1;
  border-right: none;
}

.logout {
  height: 50px;
  line-height: 50px;
  padding: 0 20px;
  color: #bfcbd9;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #1f2d3d;
}

.logout:hover {
  background-color: #263445;
  color: #409EFF;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
