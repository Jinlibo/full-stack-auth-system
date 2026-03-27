<template>
  <router-view/>
</template>

<style>
/* -----------------------------------------
   [ Product App ] Global Premium Overrides
   ----------------------------------------- */
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: none;
  overflow: hidden; /* App layout handles scroll */
}

body {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #f1f5f9;
  color: #0f172a;
}

/* Glassy/Soft Cards */
.el-card {
  border-radius: 20px !important;
  border: 1px solid #cbd5e1 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0,0,0,0.02) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  background: #ffffff !important;
}

.el-card:hover {
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08), 0 4px 6px rgba(0,0,0,0.03) !important;
  transform: translateY(-2px);
  border-color: #cbd5e1 !important;
}

.el-card__header {
  border-bottom: 1px solid #e2e8f0 !important;
  background: #ffffff !important;
  padding: 20px 24px !important;
  font-weight: 800;
  border-radius: 20px 20px 0 0 !important;
}

/* Clean Modern Tables */
.el-table {
  border-radius: 16px !important;
  overflow: hidden !important;
  box-shadow: 0 0 0 1px #cbd5e1 !important;
}
.el-table th.el-table__cell {
  background-color: #f8fafc !important;
  color: #475569 !important;
  font-weight: 800 !important;
  text-transform: uppercase;
  font-size: 13px;
  letter-spacing: 0.5px;
  height: 60px !important;
}
.el-table td.el-table__cell {
  padding-top: 18px !important;
  padding-bottom: 18px !important;
  border-bottom: 1px solid #e2e8f0 !important;
}
.el-table__row:hover > td.el-table__cell {
  background-color: #f0fdf4 !important; /* Soft green hover */
}

/* Vibrant Buttons */
.el-button {
  border-radius: 10px !important;
  font-weight: 700 !important;
  height: 40px !important;
  padding: 0 20px !important;
  transition: all 0.2s ease !important;
}
.el-button--primary {
  background: linear-gradient(135deg, #14b8a6, #0ea5e9) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(20, 184, 166, 0.25) !important;
  color: #ffffff !important;
}
.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(20, 184, 166, 0.35) !important;
  background: linear-gradient(135deg, #0d9488, #0284c7) !important;
}

/* Modern Dialogs */
.el-dialog {
  border-radius: 24px !important;
  box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25) !important;
}
.el-dialog__header {
  padding: 24px 32px 20px !important;
  border-bottom: 1px solid #f1f5f9;
  margin-right: 0 !important;
}
.el-dialog__title {
  font-weight: 800 !important;
  font-size: 20px !important;
  color: #0f172a !important;
}
.el-dialog__body {
  padding: 32px !important;
}

/* Pagination */
.el-pagination { margin-top: 24px; justify-content: flex-end; }
.el-pagination .el-pager li {
  border-radius: 10px !important; background: transparent !important; font-weight: 700; transition: all 0.2s;
}
.el-pagination .el-pager li.is-active {
  background: linear-gradient(135deg, #14b8a6, #0ea5e9) !important;
  color: #fff !important; box-shadow: 0 4px 10px rgba(20, 184, 166, 0.3);
}

/* Inputs */
.el-input__wrapper, .el-select .el-input__wrapper {
  background: #ffffff !important;
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  transition: all 0.3s ease;
  height: 40px;
}
.el-input__wrapper.is-focus, .el-select .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.2), 0 0 0 1px #0ea5e9 inset !important;
}
</style>
