import { useState, useEffect } from "react";
import { Link } from "wouter";
import { Video, Search, Lock, ArrowRight, Play, Eye, Clock } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { customFetch } from "@/lib/api-client";
import Layout from "@/components/Layout";
import { getUser } from "@/lib/auth";
import { motion } from "framer-motion";

export default function MobileLecturesPage() {
  const user = getUser();
  const [search, setSearch] = useState("");
  const [videoType, setVideoType] = useState<"all" | "live" | "recorded">("all");

  const { data: videos, isLoading } = useQuery({
    queryKey: ["videos"],
    queryFn: async () => {
      const res = await customFetch("/api/mobile/videos?type=" + videoType);
      return res.data;
    },
  });

  const videoList = Array.isArray(videos) ? videos : [];

  useEffect(() => {
    // Fetch view count tracking
  }, [videoList.length]);

  const searched = videoList.filter((v) =>
    v.title.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <Layout>
      <div className="tnc-brand-gradient text-white py-10 px-4">
        <div className="max-w-xl mx-auto">
          <h1 className="text-2xl md:text-3xl font-black mb-1">
            {videoType === "live" ? "Live Classes" : videoType === "recorded" ? "Recorded Lectures" : "All Videos"}
          </h1>
          <p className="text-white/70 text-sm">
            {videoList.length > 0 ? `${videoList.length} videos available` : "Select a course to watch lectures"}
          </p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="bg-white border-b sticky top-16 z-30">
        <div className="max-w-xl mx-auto px-4 py-3 flex flex-col sm:flex-row gap-2">
          {(["all", "live", "recorded"] as const).map((t) => (
            <button
              key={t}
              onClick={() => setVideoType(t as typeof videoType)}
              className={`flex-1 py-2 rounded-lg text-xs font-semibold transition-colors ${
                videoType === t ? "bg-blue-600 text-white" : "text-gray-500 hover:bg-gray-100"
              }`}
            >
              {t === "all" ? "All" : t === "live" ? "Live" : "Recorded"}
            </button>
          ))}
        </div>
      </div>

      {/* Videos list */}
      <div className="max-w-xl mx-auto px-4 py-8">
        {isLoading ? (
          <div className="grid grid-cols-1 gap-3">
            {[1, 2].map((i) => (
              <div key={i} className="bg-white rounded-xl overflow-hidden border h-48">
                <div className="h-48 bg-gray-200" />
              </div>
            ))}
          </div>
        ) : searched.length === 0 ? (
          <div className="text-center py-20 text-gray-500">
            <Video size={48} className="mx-auto text-gray-200 mb-3" />
            <p className="font-medium">No videos found</p>
            <p className="text-sm text-gray-400">Try adjusting your search</p>
          </div>
        ) : (
          <div className="space-y-3">
            {searched.map((video, i) => (
              <motion.div
                key={video.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="bg-white rounded-xl border border-gray-100 p-4 flex items-start justify-between hover:shadow-lg transition-shadow"
              >
                <div className="flex-1 min-w-0">
                  <h3 className="font-bold text-gray-900 text-sm truncate">{video.title}</h3>
                  <p className="text-xs text-gray-500 mt-1">
                    <Clock size={12} className="mr-1" /> {video.duration}
                  </p>
                  {video.instructor && (
                    <p className="text-xs text-gray-400">
                      <User size={12} className="mr-1" /> {video.instructor}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <Play size={16} className="text-red-500" />
                  <span className="text-sm text-gray-400">{video.views} views</span>
                </div>
                <Link
                  href={`/watch/${video.id}`}
                  className="mt-2 text-xs font-medium text-blue-600 hover:underline transition-colors"
                >
                  Watch Now <ArrowRight size={12} />
                </Link>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}