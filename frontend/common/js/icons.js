/**
 * Set minimo de iconos en SVG inline (sin libreria externa ni CDN, para no
 * anadir una dependencia de red en runtime al mismo build sin Node/npm que
 * ya usa el resto del frontend). Cada funcion admite una clase Tailwind
 * para tamano/color; usan currentColor para heredar el color de texto del
 * elemento que las contiene.
 */
function svgIcon(inner, cls) {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="${cls}" aria-hidden="true">${inner}</svg>`;
}

export const iconLogo = (cls = 'h-5 w-5') => svgIcon(
    '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>',
    cls,
);

export const iconSun = (cls = 'h-4 w-4') => svgIcon(
    '<circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>' +
    '<line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>' +
    '<line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>' +
    '<line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>',
    cls,
);

export const iconMoon = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>',
    cls,
);

export const iconEdit = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>',
    cls,
);

export const iconTrash = (cls = 'h-4 w-4') => svgIcon(
    '<polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>' +
    '<path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/>',
    cls,
);

export const iconPower = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><line x1="12" y1="2" x2="12" y2="12"/>',
    cls,
);

export const iconDownload = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/>' +
    '<line x1="12" y1="15" x2="12" y2="3"/>',
    cls,
);

export const iconCheckCircle = (cls = 'h-5 w-5') => svgIcon(
    '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
    cls,
);

export const iconXCircle = (cls = 'h-5 w-5') => svgIcon(
    '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>',
    cls,
);

export const iconUserMinus = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>' +
    '<line x1="23" y1="11" x2="17" y2="11"/>',
    cls,
);

export const iconUsers = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>' +
    '<path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    cls,
);

export const iconLogout = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/>' +
    '<line x1="21" y1="12" x2="9" y2="12"/>',
    cls,
);

export const iconChevronRight = (cls = 'h-3.5 w-3.5') => svgIcon(
    '<polyline points="9 18 15 12 9 6"/>',
    cls,
);

export const iconInbox = (cls = 'h-8 w-8') => svgIcon(
    '<polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/>' +
    '<path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>',
    cls,
);

export const iconShield = (cls = 'h-6 w-6') => svgIcon(
    '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    cls,
);

export const iconLayers = (cls = 'h-6 w-6') => svgIcon(
    '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/>' +
    '<polyline points="2 12 12 17 22 12"/>',
    cls,
);

export const iconSearch = (cls = 'h-4 w-4') => svgIcon(
    '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',
    cls,
);

export const iconCopy = (cls = 'h-3.5 w-3.5') => svgIcon(
    '<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
    cls,
);

export const iconChevronUp = (cls = 'h-3.5 w-3.5') => svgIcon(
    '<polyline points="18 15 12 9 6 15"/>',
    cls,
);

export const iconChevronDown = (cls = 'h-3.5 w-3.5') => svgIcon(
    '<polyline points="6 9 12 15 18 9"/>',
    cls,
);

export const iconGithub = (cls = 'h-4 w-4') => svgIcon(
    '<path d="M9 19c-4.3 1.4-4.3-2.5-6-3m12 5v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21"/>',
    cls,
);
