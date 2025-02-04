import { TestBed } from "@angular/core/testing";

import { FileDirectoryService } from "./file-directory.service";

describe("FileDirectoryService", () => {
  let service: FileDirectoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FileDirectoryService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
