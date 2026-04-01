import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('*/api/v1/trending', () => {
    return HttpResponse.json({
      success: true,
      data: [
        {
          videoId: 'dQw4w9WgXcQ',
          title: 'BTS SWIM Live Clip',
          channelTitle: 'BANGTANTV',
          countryCode: 'KR',
          categoryId: 10,
          categoryName: '음악',
          rankPosition: 1,
          viewCount: 15000000,
          likeCount: 500000,
          commentCount: 120000,
          publishedAt: '2026-03-31T10:00:00Z',
          thumbnailUrl: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
          duration: 'PT3M33S',
          collectedAt: '2026-04-01T12:00:00Z',
          tags: ['SURGE', 'NEW_ENTRY'],
        },
        {
          videoId: 'abc123xyz',
          title: '요즘 핫한 게임 리뷰',
          channelTitle: '게임채널',
          countryCode: 'KR',
          categoryId: 20,
          categoryName: '게임',
          rankPosition: 2,
          viewCount: 8000000,
          likeCount: 300000,
          commentCount: 50000,
          publishedAt: '2026-03-30T08:00:00Z',
          thumbnailUrl: 'https://i.ytimg.com/vi/abc123xyz/hqdefault.jpg',
          duration: 'PT10M15S',
          collectedAt: '2026-04-01T12:00:00Z',
          tags: ['HIGH_ENGAGE'],
        },
      ],
      message: null,
    });
  }),

  http.get('*/api/v1/countries', () => {
    return HttpResponse.json({
      success: true,
      data: [
        { code: 'DE', nameKo: '독일', nameEn: 'Germany', region: 'Europe' },
        { code: 'GB', nameKo: '영국', nameEn: 'United Kingdom', region: 'Europe' },
        { code: 'JP', nameKo: '일본', nameEn: 'Japan', region: 'Asia' },
        { code: 'KR', nameKo: '한국', nameEn: 'South Korea', region: 'Asia' },
        { code: 'US', nameKo: '미국', nameEn: 'United States', region: 'Americas' },
      ],
      message: null,
    });
  }),

  http.get('*/api/v1/categories', () => {
    return HttpResponse.json({
      success: true,
      data: [
        { id: 10, name: '음악' },
        { id: 17, name: '스포츠' },
        { id: 20, name: '게임' },
        { id: 22, name: '일반' },
        { id: 24, name: '엔터테인먼트' },
        { id: 25, name: '뉴스/정치' },
        { id: 26, name: '뷰티/패션' },
        { id: 27, name: '교육' },
        { id: 28, name: '과학/기술' },
      ],
      message: null,
    });
  }),

  http.get('*/api/v1/briefing', () => {
    return HttpResponse.json({
      success: true,
      data: {
        country: 'KR',
        countryName: '한국',
        summary:
          '한국 YouTube에서 K-POP 콘텐츠가 압도적 강세를 보이고 있습니다.\n게임 리뷰 콘텐츠가 높은 참여율로 주목받고 있습니다.\n엔터테인먼트 카테고리에서 신규 진입 영상이 급증하고 있습니다.',
        topVideos: [],
        generatedAt: '2026-04-01T12:00:00Z',
      },
      message: null,
    });
  }),
];
