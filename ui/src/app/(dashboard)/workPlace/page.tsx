import { LogoutButton } from '@/components/ui/LogoutButton'

export default function Dashboard() {
  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold">Dashboard</h1>
          <LogoutButton />
        </div>

        <div className="bg-card p-6 rounded-lg shadow-sm border">
          <h2 className="text-xl font-semibold mb-4">Welcome to your Dashboard!</h2>

          <div className="flex gap-4"></div>
        </div>
      </div>
    </div>
  )
}
