/**
 * Consistent section-header treatment (small gradient icon chip + title) used across cards on the
 * client detail, exercises and dashboard pages - previously every section rolled its own plain
 * `<Icon size={16}/> Title` row, which read flat against the page's otherwise glassy/gradient look.
 */
export default function SectionHeader({ icon: Icon, children, action }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-2">
      <h2 className="flex items-center gap-2.5 font-semibold">
        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-brand-orange/25 to-brand-orangeDeep/10 text-brand-orange ring-1 ring-brand-orange/20">
          <Icon size={14} />
        </span>
        {children}
      </h2>
      {action}
    </div>
  );
}
