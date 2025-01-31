import { Component } from "@angular/core";
import { UserService } from "../../common/service/user/user.service";
import { User } from "../../common/type/user";

@Component({
  selector: "texera-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.scss"]
})
export class ProfileComponent {
  public user: User | undefined;
  public scpUsername : string | undefined;
  public scpPassword: string | undefined;
  public showPassword: boolean = false;

  constructor(
    private userService: UserService,
  ) {
    this.user = this.userService.getCurrentUser();

    if (this.user) {
      // this.scpUsername = this.userService.getSCPUsername();
      this.scpUsername = this.userService.getSCPUsername();
      this.scpPassword = this.userService.getSCPPassword();
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  regeneratePassword(): void {
    this.scpPassword = this.userService.regenerateSCPPassword();
  }
}
