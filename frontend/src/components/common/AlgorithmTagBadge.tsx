import { TAG_CONFIG } from '@/types';
import type { AlgorithmTag } from '@/types';

interface Props {
  tag: string;
}

export default function AlgorithmTagBadge({ tag }: Props) {
  const config = TAG_CONFIG[tag as AlgorithmTag];
  if (!config) return null;

  return (
    <span
      data-cy="algorithm-tag"
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium ${config.color}`}
    >
      <span>{config.emoji}</span>
      <span>{config.label}</span>
    </span>
  );
}
