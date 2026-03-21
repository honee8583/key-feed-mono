import { useState } from 'react';
import { TrendingUp, Users, Zap, Loader2 } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { useTrendingKeywords, useRecommendedSources } from '../api/exploreApi';
// To trigger search from Explore, we'll need search integration later
// The base implementation fired 'handleSearchTrigger' directly from App.tsx. I will mock that logic via state update to be routed when Search feature is ready.

export function ExploreTab() {
    const { openSearch } = useUiStore();
    const [subscribed, setSubscribed] = useState<number[]>([]);

    // API 연결
    const { data: trendingKeywords, status } = useTrendingKeywords();
    const { data: recommendedFeeds, status: recommendedStatus } = useRecommendedSources();

    const toggleSubscribe = (id: number) => {
        setSubscribed(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
    };

    const handleKeywordClick = (keyword: string) => {
        // Suppressing unused param
        console.log(keyword);
        openSearch();
    };

    return (
        <div className="px-5 pt-2 pb-24">
            <section className="mb-6">
                <div className="flex items-center gap-2 mb-2 px-1">
                    <TrendingUp size={16} className="text-slate-500" />
                    <h3 className="text-xs font-black text-slate-600 uppercase tracking-tight">
                        실시간 트렌딩
                    </h3>
                </div>
                <div className="bg-white/40 backdrop-blur-xl rounded-2xl border border-white/60 overflow-hidden shadow-sm">
                    {status === 'pending' ? (
                        <div className="flex justify-center items-center py-8">
                            <Loader2 className="w-5 h-5 animate-spin text-slate-400" />
                        </div>
                    ) : status === 'error' ? (
                        <div className="text-center py-8 text-[11px] text-slate-500 font-medium">
                            트렌딩 키워드를 불러올 수 없습니다.
                        </div>
                    ) : (
                        trendingKeywords?.map((item, index) => {
                            const rank = index + 1;
                            return (
                                <div
                                    key={item.name}
                                    onClick={() => handleKeywordClick(item.name)}
                                    className="flex items-center justify-between p-3 hover:bg-white/30 transition-colors cursor-pointer border-b border-white/10 last:border-none"
                                >
                                    <div className="flex items-center gap-3">
                                        <span className={`text-[11px] font-black w-3 text-center ${rank <= 3 ? 'text-slate-900' : 'text-slate-300'}`}>
                                            {rank}
                                        </span>
                                        <span className="text-[13px] font-bold text-slate-700">
                                            {item.name}
                                        </span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <span className="text-[10px] font-bold text-slate-400">
                                            {item.userCount.toLocaleString()}명
                                        </span>
                                        <span className="text-[8px] font-bold text-slate-300">-</span>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </section>

            <section>
                <div className="flex items-center gap-2 mb-2 px-1">
                    <Users size={16} className="text-slate-500" />
                    <h3 className="text-xs font-black text-slate-600 uppercase tracking-tight">
                        추천 블로그
                    </h3>
                </div>
                <div className="space-y-2">
                    {recommendedStatus === 'pending' ? (
                        <div className="flex justify-center items-center py-6 bg-white/40 backdrop-blur-xl rounded-2xl border border-white/60">
                            <Loader2 className="w-5 h-5 animate-spin text-slate-400" />
                        </div>
                    ) : recommendedStatus === 'error' ? (
                        <div className="text-center py-6 text-[11px] text-slate-500 font-medium bg-white/40 backdrop-blur-xl rounded-2xl border border-white/60">
                            추천 소스를 불러올 수 없습니다.
                        </div>
                    ) : (
                        recommendedFeeds?.map((feed) => {
                            let hostname = feed.url;
                            try {
                                hostname = new URL(feed.url).hostname;
                            } catch (e) {
                                // Ignore
                            }

                            return (
                                <div key={feed.sourceId} className="bg-white/40 backdrop-blur-xl p-3.5 rounded-2xl border border-white/60 shadow-sm flex items-center justify-between">
                                    <div className="flex items-center gap-3">
                                        <div className="w-9 h-9 bg-white/50 rounded-xl flex items-center justify-center border border-white/60 text-slate-400">
                                            <Zap size={16} />
                                        </div>
                                        <div>
                                            <h4 className="text-[13px] font-black text-slate-800 leading-none mb-1">
                                                {hostname}
                                            </h4>
                                            <p className="text-[9px] text-slate-400 font-medium line-clamp-1">
                                                {feed.subscriberCount.toLocaleString()} Subscribers
                                            </p>
                                        </div>
                                    </div>
                                    <button
                                        onClick={() => toggleSubscribe(feed.sourceId)}
                                        className={`px-3 py-1.5 rounded-lg text-[9px] font-black uppercase transition-all ${subscribed.includes(feed.sourceId) ? 'bg-slate-200 text-slate-500' : 'bg-slate-900 text-white shadow-sm'}`}
                                    >
                                        {subscribed.includes(feed.sourceId) ? '구독중' : '구독'}
                                    </button>
                                </div>
                            );
                        })
                    )}
                </div>
            </section>
        </div>
    );
}
