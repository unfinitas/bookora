export function Footer() {
  return (
    <footer className="border-t border-border/40">
      <div className="container mx-auto px-4 py-8">
        <p className="text-center text-sm text-muted-foreground">
          © Bookora {new Date().getFullYear()}
        </p>
      </div>
    </footer>
  )
}
