import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormBuilder, FormGroup, FormArray, Validators } from "@angular/forms";

@Component({
  selector: "texera-user-contributors-form",
  templateUrl: "./contributors-form.component.html",
  styleUrls: ["./contributors-form.component.scss"],
})

export class ContributorsFormComponent {
  contributorsForm!: FormGroup;

  // List of contributor roles
  contributorRoles = [
    "Contact Person",
    "Data Collector",
    "Data Curator",
    "Project Leader",
    "Project Manager",
    "Project Member",
    "Related Person",
    "Researcher",
    "Research Group",
    "Other"
  ];

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
    this.contributorsForm = this.fb.group({
      contributors: this.fb.array([]) // Initialize FormArray
    });
  }

  // Getter for the form array
  get contributors() {
    return (this.contributorsForm?.get("contributors") as FormArray);
  }

  // Add a new contributor to the FormArray
  addContributor() {
    const contributorGroup = this.fb.group({
      name: ["", Validators.required],
      role: ["", Validators.required] // Contributor type
    });

    this.contributors.push(contributorGroup);
  }

  // remove a contributor from the FormArray
  removeContributor(index: number) {
    this.contributors.removeAt(index);
  }
}
