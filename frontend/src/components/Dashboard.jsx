import React from 'react';
import SummaryCards from './SummaryCards.jsx';
import AuthorLeaderboard from './AuthorLeaderboard.jsx';
import {
  MonthlyTrend, HourChart, WeekdayChart, FileChurnChart, VerbChart, FileTypeChart,
} from './Charts.jsx';
import { BusFactorTable, LargestCommitsTable, WeekdayByAuthorTable } from './Tables.jsx';

export default function Dashboard({ data }) {
  if (!data) return null;

  return (
    <div className="dashboard">
      <div className="section-label">Key metrics</div>
      <SummaryCards summary={data.summary} />

      <div className="section-label">Activity trends</div>
      <MonthlyTrend data={data.commitsByMonth} />
      <div className="grid-2">
        <HourChart data={data.commitsByHour} />
        <WeekdayChart data={data.commitsByWeekday} />
      </div>

      <div className="section-label">Contributors</div>
      <AuthorLeaderboard authors={data.authors} />
      <WeekdayByAuthorTable data={data.weekdayByAuthor} />

      <div className="section-label">Codebase composition</div>
      <div className="grid-2">
        <FileChurnChart data={data.fileChurn} />
        <VerbChart data={data.commitVerbs} />
      </div>
      <div className="grid-2">
        <FileTypeChart data={data.fileTypes} />
        <BusFactorTable data={data.busFactor} />
      </div>

      <div className="section-label">Notable commits</div>
      <LargestCommitsTable data={data.largestCommits} />
    </div>
  );
}
