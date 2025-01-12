package GoingUp.Features;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bank {
    int prePark = 2;
    int preCapital =2;
    int preMCar=2;
    int preTour = 2;
    int preEat = 2;
    int preSCar = 2;
    int preBank = 2;
    int prePharmacy = 2;
    int preDeath = 2;
    int preBuild = 2;
    String currentPriority = "";
    String nextPriority = "";

    int quPark = 80;
    int quCapital = 80;
    int quMCar = 80;
    int quTour = 80;
    int quEat = 80;
    int quSCar = 80;
    int quBank = 80;
    int quPharmacy = 80;
    int quDeath = 80;
    int quBuild = 80;

    public void initPreBuyStart() {
        prePark = 2;
        preCapital = 2;
        preMCar = 2;
        preTour = 2;
        preEat = 2;
        preSCar = 2;
        preBank = 2;
        prePharmacy = 2;
        preDeath = 2;
        preBuild = 2;
        currentPriority = nextPriority;
        nextPriority = "";
    }

}
