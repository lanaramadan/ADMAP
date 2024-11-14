import { Component, OnInit } from "@angular/core";
import { Observable } from "rxjs";
import { HubWorkflowService } from "../../service/workflow/hub-workflow.service";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { Router } from "@angular/router";
import { DashboardWorkflow } from "../../../dashboard/type/dashboard-workflow.interface";
import { SearchService } from "../../../dashboard/service/user/search.service";
import { DashboardEntry, UserInfo } from "../../../dashboard/type/dashboard-entry";
import { map, switchMap } from "rxjs/operators";

@UntilDestroy()
@Component({
  selector: "texera-landing-page",
  templateUrl: "./landing-page.component.html",
  styleUrls: ["./landing-page.component.scss"],
})
export class LandingPageComponent implements OnInit {
  public databaseCount: number = 0;
  public topLovedDatabases: DashboardEntry[] = [];
  public topClonedDatabases: DashboardEntry[] = [];

  constructor(
    private hubDatabaseService: HubWorkflowService,
    private router: Router,
    private searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.getDatabaseCount();
    this.fetchTopDatabases(
      // TODO: getTopLovedWorkflows should eventually be replaced with getTopLovedWDatabases
      this.hubDatabaseService.getTopLovedWorkflows(),
      databases => (this.topLovedDatabases = databases),
      "Top Loved Databases"
    );
    this.fetchTopDatabases(
      // TODO: getTopClonedWorkflows should eventually be replaced with topClonedWDatabases
      this.hubDatabaseService.getTopClonedWorkflows(),
      databases => (this.topClonedDatabases = databases),
      "Top Cloned Databases"
    );
  }

  getDatabaseCount(): void {
    this.hubDatabaseService
      // TODO: getTopClonedWorkflows should eventually be replaced with topClonedWDatabases
      .getWorkflowCount()
      .pipe(untilDestroyed(this))
      .subscribe((count: number) => {
        this.databaseCount = count;
      });
  }

  /**
   * Helper function to fetch top databases and associate user info with them.
   * @param databasesObservable Observable that returns databases (Top Loved or Top Cloned)
   * @param updateDatabasesFn Function to update the component's database state
   * @param databaseType Label for logging
   */
  fetchTopDatabases(
    databasesObservable: Observable<DashboardWorkflow[]>,
    updateDatabasesFn: (entries: DashboardEntry[]) => void,
    databaseType: string
  ): void {
    databasesObservable
      .pipe(
        // eslint-disable-next-line rxjs/no-unsafe-takeuntil
        untilDestroyed(this),
        map((databases: DashboardWorkflow[]) => {
          const userIds = new Set<number>();
          databases.forEach(database => {
            userIds.add(database.ownerId);
          });
          return { databases, userIds: Array.from(userIds) };
        }),
        switchMap(({ databases, userIds }) =>
          this.searchService.getUserInfo(userIds).pipe(
            map((userIdToInfoMap: { [key: number]: UserInfo }) => {
              const dashboardEntries = databases.map(database => {
                const userInfo = userIdToInfoMap[database.ownerId];
                const entry = new DashboardEntry(database);
                if (userInfo) {
                  entry.setOwnerName(userInfo.userName);
                  entry.setOwnerGoogleAvatar(userInfo.googleAvatar ?? "");
                }
                return entry;
              });
              return dashboardEntries;
            })
          )
        )
      )
      .subscribe((dashboardEntries: DashboardEntry[]) => {
        updateDatabasesFn(dashboardEntries);
      });
  }

  navigateToSearch(): void {
    this.router.navigate(["/dashboard/hub/database/result"]);
  }
}
