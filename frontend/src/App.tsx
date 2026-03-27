import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { GeneratePage } from './pages/generate/GeneratePage'
import { HealthPage } from './pages/health/HealthPage'
import { LorebookPage } from './pages/lorebook/LorebookPage'
import { ReaderPage } from './pages/reader/ReaderPage'
import { StoryCreatePage } from './pages/story-create/StoryCreatePage'
import { StoryDetailPage } from './pages/story-detail/StoryDetailPage'
import { StoryListPage } from './pages/story-list/StoryListPage'
import { StoryStateDebugPage } from './pages/state-debug/StoryStateDebugPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<AppLayout />}>
        <Route index element={<StoryListPage />} />
        <Route path="stories/new" element={<StoryCreatePage />} />
        <Route path="stories/:storyId/generate" element={<GeneratePage />} />
        <Route path="stories/:storyId/read" element={<ReaderPage />} />
        <Route path="stories/:storyId/state" element={<StoryStateDebugPage />} />
        <Route path="stories/:storyId" element={<StoryDetailPage />} />
        <Route path="lorebook" element={<LorebookPage />} />
        <Route path="health" element={<HealthPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
