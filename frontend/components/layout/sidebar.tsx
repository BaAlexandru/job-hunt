"use client"

import { usePathname } from "next/navigation"
import Link from "next/link"
import { cn } from "@/lib/utils"
import {
  LayoutDashboard,
  Briefcase,
  Building2,
  FileText,
  FolderOpen,
  Globe,
  Users,
} from "lucide-react"
import { Separator } from "@/components/ui/separator"

export const mainNavItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/applications", label: "Applications", icon: Briefcase },
  { href: "/companies", label: "Companies", icon: Building2 },
  { href: "/jobs", label: "Jobs", icon: FileText },
  { href: "/documents", label: "Documents", icon: FolderOpen },
]

export const secondaryNavItems = [
  { href: "/browse", label: "Browse", icon: Globe },
  { href: "/shared", label: "Shared with me", icon: Users },
]

/** @deprecated Use mainNavItems and secondaryNavItems instead */
export const navItems = mainNavItems

export function Sidebar() {
  const pathname = usePathname()

  function renderNavLink(item: (typeof mainNavItems)[number]) {
    return (
      <Link
        key={item.href}
        href={item.href}
        className={cn(
          "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          pathname === item.href
            ? "bg-accent text-accent-foreground"
            : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
        )}
      >
        <item.icon className="h-4 w-4" />
        {item.label}
      </Link>
    )
  }

  return (
    <aside className="hidden fixed inset-y-0 left-0 z-40 w-64 flex-col border-r bg-background md:flex">
      <div className="p-6">
        <h1 className="text-xl font-semibold">JobHunt</h1>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {mainNavItems.map(renderNavLink)}
        <Separator className="my-2 mx-3" />
        {secondaryNavItems.map(renderNavLink)}
      </nav>
    </aside>
  )
}
