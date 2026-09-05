import { Box, Container, Link, Stack, Typography } from '@mui/material';
import { ButtonsSection } from '../components/preview/ButtonsSection';
import { ColorsSection } from '../components/preview/ColorsSection';
import { DataDisplaySection } from '../components/preview/DataDisplaySection';
import { FeedbackSection } from '../components/preview/FeedbackSection';
import { FormsSection } from '../components/preview/FormsSection';
import { NavigationSection } from '../components/preview/NavigationSection';
import { PlatformSection } from '../components/preview/PlatformSection';
import { PreviewShell, sections } from '../components/preview/PreviewShell';
import { SurfacesSection } from '../components/preview/SurfacesSection';
import { TypographySection } from '../components/preview/TypographySection';

const SectionIndex = () => (
  <Stack
    component="nav"
    spacing={0.25}
    sx={{ position: 'sticky', top: 88, display: { xs: 'none', lg: 'flex' }, width: 148, flexShrink: 0 }}
  >
    <Typography variant="overline" sx={{ color: 'text.disabled', mb: 0.5 }}>
      On this page
    </Typography>
    {sections.map((section) => (
      <Link
        key={section.id}
        href={`#${section.id}`}
        underline="none"
        sx={{
          typography: 'body2',
          color: 'text.secondary',
          py: 0.5,
          px: 1,
          borderRadius: 1,
          borderLeft: 2,
          borderColor: 'border.subtle',
          '&:hover': { color: 'text.primary', backgroundColor: 'surface.hover', borderColor: 'primary.main' },
        }}
      >
        {section.label}
      </Link>
    ))}
  </Stack>
);

const PageHeader = () => (
  <Stack spacing={1.5} sx={{ mb: 7 }}>
    <Typography variant="overline" sx={{ color: 'brand.ember' }}>
      Design system · v0.1
    </Typography>
    <Typography variant="h1">CodeForge theme preview</Typography>
    <Typography variant="body1" sx={{ color: 'text.secondary', maxWidth: 760 }}>
      Every component in the system, on one page, with no logic behind it. This route is a scaffold for reviewing the
      theme and is meant to be deleted once the real product pages land.
    </Typography>
  </Stack>
);

/** Throwaway route: a visual index of the whole design system. */
export const ThemePreviewPage = () => (
  <PreviewShell>
    <Container maxWidth="xl" sx={{ py: 6 }}>
      <Stack direction="row" spacing={6}>
        <SectionIndex />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <PageHeader />
          <Stack spacing={7}>
            <ColorsSection />
            <TypographySection />
            <SurfacesSection />
            <ButtonsSection />
            <FormsSection />
            <FeedbackSection />
            <DataDisplaySection />
            <NavigationSection />
            <PlatformSection />
          </Stack>
        </Box>
      </Stack>
    </Container>
  </PreviewShell>
);
