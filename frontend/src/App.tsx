import { Routes, Route, Outlet } from 'react-router-dom';
import NavBar from './components/layout/NavBar';
import DashboardPage from './pages/DashboardPage';
import TrendingPage from './pages/TrendingPage';
import ChannelsPage from './pages/ChannelsPage';
import ChannelDetailPage from './pages/ChannelDetailPage';
import KeywordsPage from './pages/KeywordsPage';
import CrossBorderPage from './pages/CrossBorderPage';
import DetailPage from './pages/DetailPage';

function Layout() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />
      <main className="max-w-[1280px] mx-auto px-4 md:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
}

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/trending" element={<TrendingPage />} />
        <Route path="/channels" element={<ChannelsPage />} />
        <Route path="/channels/:channelId" element={<ChannelDetailPage />} />
        <Route path="/keywords" element={<KeywordsPage />} />
        <Route path="/crossborder" element={<CrossBorderPage />} />
        <Route path="/video/:videoId" element={<DetailPage />} />
      </Route>
    </Routes>
  );
}

export default App;
