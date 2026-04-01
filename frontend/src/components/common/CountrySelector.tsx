import { useFilterStore } from '@/stores/filterStore';
import { useCountries } from '@/hooks/useCountries';
import { COUNTRY_FLAGS } from '@/types';

export default function CountrySelector() {
  const { country, setCountry } = useFilterStore();
  const { data: countries } = useCountries();

  return (
    <div className="flex gap-2 flex-wrap" data-cy="country-selector">
      {countries?.map((c) => (
        <button
          key={c.code}
          data-cy={`country-${c.code}`}
          onClick={() => setCountry(c.code)}
          className={`flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-all ${
            country === c.code
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'bg-muted text-muted-foreground hover:bg-accent'
          }`}
        >
          <span className="text-lg">{COUNTRY_FLAGS[c.code] || '🏳️'}</span>
          <span>{c.nameKo}</span>
        </button>
      ))}
    </div>
  );
}
