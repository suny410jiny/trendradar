import { create } from 'zustand';

interface FilterState {
  country: string;
  category: number | null;
  tag: string | null;
  setCountry: (country: string) => void;
  setCategory: (category: number | null) => void;
  setTag: (tag: string | null) => void;
}

export const useFilterStore = create<FilterState>((set) => ({
  country: 'KR',
  category: null,
  tag: null,
  setCountry: (country) => set({ country }),
  setCategory: (category) => set({ category }),
  setTag: (tag) => set({ tag }),
}));
