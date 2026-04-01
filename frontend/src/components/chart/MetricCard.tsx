interface Props {
  title: string;
  value: string;
  icon: string;
  subtitle?: string;
}

export default function MetricCard({ title, value, icon, subtitle }: Props) {
  return (
    <div className="rounded-xl border border-border bg-card p-4" data-cy="metric-card">
      <div className="flex items-center gap-2 text-muted-foreground mb-1">
        <span>{icon}</span>
        <span className="text-xs font-medium">{title}</span>
      </div>
      <p className="text-2xl font-bold text-foreground">{value}</p>
      {subtitle && (
        <p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>
      )}
    </div>
  );
}
