export default function HomePage() {
  return (
    <div className="min-h-screen bg-background" data-cy="home-page">
      <header className="border-b border-border px-6 py-4">
        <h1 className="text-2xl font-bold text-foreground">TrendRadar</h1>
        <p className="text-sm text-muted-foreground">세상의 트렌드를 레이더처럼 감지하다</p>
      </header>
      <main className="p-6">
        <p className="text-muted-foreground">PHASE 8에서 화면을 구현합니다.</p>
      </main>
    </div>
  );
}
