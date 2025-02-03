import {Component, OnInit} from "@angular/core";
import { FileDirectoryService } from "../../../service/user/file-directory/file-directory.service";

@Component({
  selector: "texera-file-directory",
  templateUrl: "./file-directory.component.html",
  styleUrls: ["./file-directory.component.css"]
})
export class FileDirectoryComponent implements OnInit {
    directoryHtml: string = "";

  constructor(private fileDirectoryService: FileDirectoryService) {}

ngOnInit(): void {
    this.fileDirectoryService.fetchDirectories().subscribe(
        (data: string) => {
            this.directoryHtml = data;
        },
        (error) => {
            console.error("Error fetching directory:", error);
        }
    );
}
}
