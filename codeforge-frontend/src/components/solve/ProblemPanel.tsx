import { Stack, Tab, Tabs } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type ProblemDetail } from '../../api/types';
import { EditorialPanel } from './EditorialPanel';
import { ProblemDescription } from './ProblemDescription';
import { SubmissionsPanel } from './SubmissionsPanel';

type PanelTab = 'description' | 'editorial' | 'submissions';

/**
 * The left-hand half of the solving page.
 *
 * <p>Owns only which tab is showing; each tab is a component of its own, so the
 * submission history fetches nothing while the description is on screen.
 */
export const ProblemPanel = ({ problem, refreshKey }: { problem: ProblemDetail; refreshKey: number }) => {
  const { t } = useTranslation();
  const [tab, setTab] = useState<PanelTab>('description');

  return (
    <Stack sx={{ height: '100%' }}>
      <Tabs
        value={tab}
        onChange={(_, next: PanelTab) => {
          setTab(next);
        }}
        sx={{ px: 1, borderBottom: 1, borderColor: 'border.subtle', flexShrink: 0 }}
      >
        <Tab value="description" label={t('solve.description')} />
        {/* Disabled rather than hidden when nothing is written: the tab's absence
            would read as a missing feature rather than as missing content. */}
        <Tab value="editorial" label={t('solve.editorial')} disabled={!problem.hasEditorial} />
        <Tab value="submissions" label={t('solve.submissions')} />
      </Tabs>

      <Stack sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        {tab === 'description' ? (
          <ProblemDescription problem={problem} />
        ) : tab === 'editorial' ? (
          <EditorialPanel slug={problem.slug} />
        ) : (
          <SubmissionsPanel slug={problem.slug} refreshKey={refreshKey} />
        )}
      </Stack>
    </Stack>
  );
};
