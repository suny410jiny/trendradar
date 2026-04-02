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
    <nav className="sticky top-0 z-50 bg-gray-900/95 backdrop-blur border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-14">
          {/* Logo */}
          <NavLink
            to="/"
            className="text-lg font-bold text-blue-400 flex items-center gap-2 shrink-0"
          >
            📡 TrendRadar
          </NavLink>

          {/* Nav Links */}
          <div className="flex items-center gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  `px-3 py-2 text-sm rounded-lg transition-colors ${
                    isActive
                      ? 'bg-blue-500/10 text-blue-400 font-medium'
                      : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800'
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
                className={`px-2 py-1 text-sm rounded transition-colors ${
                  country === code
                    ? 'bg-blue-500/20 text-blue-400'
                    : 'text-gray-500 hover:text-gray-300'
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
