import { CommunityDetail } from "@/components/community-detail"

export default async function CommunityDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return <main className="page-main"><div className="site-shell"><CommunityDetail questionId={Number(id)} /></div></main>
}
