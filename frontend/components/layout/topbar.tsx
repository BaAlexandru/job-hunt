"use client"

import { useState } from "react"
import { UserButton } from "@daveyplate/better-auth-ui"
import { Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { MobileNav } from "./mobile-nav"

export function Topbar() {
  const [open, setOpen] = useState(false)

  return (
    <header className="flex h-14 items-center justify-between border-b px-6">
      <div className="md:hidden">
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon">
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-64 p-0">
            <MobileNav onNavigate={() => setOpen(false)} />
          </SheetContent>
        </Sheet>
      </div>
      <div className="flex-1" />
      <UserButton />
    </header>
  )
}
