import { BrowserRouter, Routes, Route } from "react-router-dom"

import LoginPage from "@/pages/login/Login"
import AuthCallback from "@/pages/auth/callback/AuthCallback"

import DashboardPage from "@/pages/dashboard/Dashboard"
import OverviewPage from "@/pages/dashboard/overview/Overview"
import SettingsPage from "@/pages/dashboard/settings/Settings"
import ChatPage from "@/pages/chat/ChatPage"
import HomePage from "./pages/home/HomePage"

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallback />} />

        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/dashboard/overview" element={<OverviewPage />} />
        <Route path="/dashboard/settings" element={<SettingsPage />} />

        <Route path="/chat" element={<ChatPage />} />
        <Route path="/chat/:repoId" element={<ChatPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App