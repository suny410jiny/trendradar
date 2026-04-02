import { NavLink } from 'react-router-dom';
import { useFilterStore } from '../../stores/filterStore';
import { COUNTRY_FLAGS } from '../../types';

const NAV_ITEMS = [
  { path: '/', label: '대시보드' },
  { path: '/trending', label: '트렌딩' },
  { path: '/channels', label: '채널 랭킹' },
  { path: '/keywords', label: '키워드' },
  { path: '/crossborder', label: '크로스보더' },
];

const COUNTRIES = ['KR', 'US', 'JP', 'GB', 'DE'];

export default function NavBar() {
  const { country, setCountry } = useFilterStore();

  return (
    <nav
      className="sticky top-0 z-50 border-b border-border/60 bg-background/88 backdrop-blur-xl"
      data-cy="navbar"
    >
      <div className="max-w-[1280px] mx-auto px-4 md:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <NavLink
            to="/"
            className="text-[22px] font-black tracking-tight shrink-0"
            style={{ fontFamily: 'var(--font-heading)' }}
          >
            <span className="text-primary">Trend</span>
            <span className="text-accent">Radar</span>
          </NavLink>

          {/* Nav Links */}
          <div className="flex items-center gap-0.5">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  `px-4 py-2 text-[13px] font-semibold rounded-3xl transition-colors ${
                    isActive
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>

          {/* Country Selector */}
          <div className="flex items-center gap-1 shrink-0">
            {COUNTRIES.map((code) => (
              <button
                key={code}
                onClick={() => setCountry(code)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-2xl border transition-colors ${
                  country === code
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'text-muted-foreground border-transparent hover:border-border'
                }`}
              >
                {COUNTRY_FLAGS[code]} {code}
              </button>
            ))}
          </div>
        </div>
      </div>
    </nav>
  );
}
