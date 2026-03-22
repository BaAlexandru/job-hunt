"use client"

import { usePathname } from "next/navigation"
import Link from "next/link"
import { cn } from "@/lib/utils"
import { mainNavItems, secondaryNavItems } from "./sidebar"
import { Separator } from "@/components/ui/separator"

export function MobileNav({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()

  function renderNavLink(item: (typeof mainNavItems)[number]) {
    return (
      <Link
        key={item.href}
        href={item.href}
        onClick={onNavigate}
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
    <div className="flex flex-col">
      <div className="p-6">
        <h2 className="text-xl font-semibold">JobHunt</h2>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {mainNavItems.map(renderNavLink)}
        <Separator className="my-2 mx-3" />
        {secondaryNavItems.map(renderNavLink)}
      </nav>
    </div>
  )
}
