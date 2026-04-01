import { create } from 'zustand';

interface FilterState {
  country: string;
  category: number | null;
  setCountry: (country: string) => void;
  setCategory: (category: number | null) => void;
}

export const useFilterStore = create<FilterState>((set) => ({
  country: 'KR',
  category: null,
  setCountry: (country) => set({ country }),
  setCategory: (category) => set({ category }),
}));
