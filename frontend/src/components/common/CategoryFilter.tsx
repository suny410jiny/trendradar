import { useFilterStore } from '@/stores/filterStore';
import { useCategories } from '@/hooks/useCategories';

export default function CategoryFilter() {
  const { category, setCategory } = useFilterStore();
  const { data: categories } = useCategories();

  return (
    <div className="flex gap-2 flex-wrap" data-cy="category-filter">
      <button
        onClick={() => setCategory(null)}
        className={`rounded-full px-3 py-1 text-sm font-medium transition-all ${
          category === null
            ? 'bg-primary text-primary-foreground'
            : 'bg-muted text-muted-foreground hover:bg-accent'
        }`}
      >
        전체
      </button>
      {categories?.map((cat) => (
        <button
          key={cat.id}
          data-cy={`category-${cat.id}`}
          onClick={() => setCategory(cat.id)}
          className={`rounded-full px-3 py-1 text-sm font-medium transition-all ${
            category === cat.id
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted text-muted-foreground hover:bg-accent'
          }`}
        >
          {cat.name}
        </button>
      ))}
    </div>
  );
}
