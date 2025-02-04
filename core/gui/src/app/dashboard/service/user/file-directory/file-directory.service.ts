import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import {DATASET_BASE_URL} from "../dataset/dataset.service";
import {DashboardDataset} from "../../../type/dashboard-dataset.interface";
import {AppSettings} from "../../../../common/app-setting";

export const DIRECTORY_BASE_URL = "directory";



@Injectable({
  providedIn: "root"
})
export class FileDirectoryService {
    constructor(private http: HttpClient) {}

    public fetchDirectories(): Observable<string> {
        return this.http.get(`${AppSettings.getApiEndpoint()}/${DIRECTORY_BASE_URL}`, { responseType: "text" });
    }
}
