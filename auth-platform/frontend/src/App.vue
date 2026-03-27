<template>
  <router-view/>
</template>

<style>
/* -----------------------------------------
   [ Auth Platform ] Global Premium Overrides
   ----------------------------------------- */
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: none;
  overflow: hidden; /* App Layout will handle scroll */
}

body {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #e4e4e7;
  color: #18181b;
}

/* Premium Element Plus Card */
.el-card {
  border-radius: 16px !important;
  border: 1px solid #d4d4d8 !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03) !important;
  background-color: #ffffff !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.el-card:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -2px rgba(0, 0, 0, 0.04) !important;
  transform: translateY(-2px);
}

.el-card__header {
  border-bottom: 1px solid #d4d4d8 !important;
  font-weight: 600;
  font-size: 16px;
  padding: 16px 20px !important;
}

/* Minimalist Table */
.el-table {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #d4d4d8 !important;
  background-color: #ffffff;
}
.el-table th.el-table__cell {
  background-color: #f8fafc !important;
  color: #64748b !important;
  font-weight: 700 !important;
  height: 54px !important;
  text-transform: uppercase;
  font-size: 13px;
  letter-spacing: 0.5px;
}
.el-table td.el-table__cell {
  padding-top: 16px !important;
  padding-bottom: 16px !important;
  border-bottom: 1px solid #d4d4d8 !important;
}
.el-table__row:hover > td.el-table__cell {
  background-color: #f8fafc !important;
}

/* Desktop Premium Buttons */
.el-button {
  border-radius: 8px !important;
  font-weight: 600 !important;
  transition: all 0.2s ease !important;
  height: 38px !important;
  padding: 0 16px !important;
}
.el-button--primary {
  background: linear-gradient(135deg, #3b82f6, #60a5fa) !important;
  border: none !important;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2) !important;
  color: #ffffff !important;
}
.el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 15px rgba(59, 130, 246, 0.3) !important;
  background: linear-gradient(135deg, #2563eb, #3b82f6) !important;
}

.el-button [class*="el-icon"] + span { margin-left: 6px; }

/* Modern Dialogs */
.el-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25) !important;
}
.el-dialog__header {
  margin-right: 0 !important;
  padding: 24px 28px 20px !important;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}
.el-dialog__title {
  font-weight: 800 !important;
  font-size: 18px !important;
  color: #09090b !important;
}
.el-dialog__body {
  padding: 32px 28px !important;
}

/* Pagination */
.el-pagination {
  margin-top: 24px;
  justify-content: flex-end;
}
.el-pagination .el-pager li {
  border-radius: 8px !important;
  background: transparent !important;
  font-weight: 600;
  transition: all 0.2s;
}
.el-pagination .el-pager li.is-active {
  background: #3b82f6 !important;
  color: #fff !important;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

/* Inputs / Forms */
.el-input__wrapper, .el-select .el-input__wrapper {
  background: #f8fafc !important;
  border-radius: 8px !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  transition: all 0.3s ease;
}
.el-input__wrapper.is-focus, .el-select .el-input__wrapper.is-focus {
  background: #ffffff !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2), 0 0 0 1px #3b82f6 inset !important;
}
</style>
