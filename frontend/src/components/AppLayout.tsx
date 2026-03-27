import { Link, Outlet } from 'react-router-dom'

export function AppLayout() {
  return (
    <div className="min-h-screen bg-base-200 text-base-content">
      <header className="navbar bg-base-100 shadow-md px-4">
        <div className="flex-1">
          <Link
            to="/"
            className="btn btn-ghost text-lg font-semibold tracking-tight"
          >
            EroticaForge
          </Link>
        </div>
        <nav className="flex-none flex flex-wrap items-center justify-end gap-1">
          <Link to="/" className="btn btn-ghost btn-sm">
            故事列表
          </Link>
          <Link to="/lorebook" className="btn btn-ghost btn-sm">
            Lorebook
          </Link>
          <Link to="/health" className="btn btn-ghost btn-sm">
            服务状态
          </Link>
          <Link to="/stories/new" className="btn btn-primary btn-sm">
            新建故事
          </Link>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-5xl px-4 py-8">
        <Outlet />
      </main>

      <footer className="footer footer-center border-t border-base-300 bg-base-100 p-4 text-sm text-base-content/60">
        <p>
          本地工具 · API{' '}
          <code className="rounded bg-base-200 px-1 py-0.5 text-xs">/api</code>
          {' · '}
          <Link to="/health" className="link link-hover">
            自检
          </Link>
        </p>
      </footer>
    </div>
  )
}
