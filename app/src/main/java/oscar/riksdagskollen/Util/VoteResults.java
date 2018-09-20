package oscar.riksdagskollen.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by oscar on 2018-09-20.
 */


/**
 * Class for parsing and getting the voteresults.
 */
public class VoteResults {
    private HashMap<String, int[]> voteResults = new HashMap<>();
    private ArrayList<String> partiesInVoteList = new ArrayList<>();

    public VoteResults(HashMap<String, int[]> voteResults) {
        this.voteResults = voteResults;
    }

    public VoteResults(String response) {
        Document doc = Jsoup.parse(response);
        //This is probably a really bad way of doing this.
        String allVotesString = doc.getElementsByClass("vottabell").get(0).text().split("Frånvarande")[1];
        String allVotesArr[] = allVotesString.split(" ");
        for (int i = 1; i < allVotesArr.length; i=i+5) {
            try {

                int[] data = {Integer.valueOf(allVotesArr[i + 1]),
                        Integer.valueOf(allVotesArr[i + 2]),
                        Integer.valueOf(allVotesArr[i + 3]),
                        Integer.valueOf(allVotesArr[i + 4])};
                String label = allVotesArr[i].toUpperCase();
                voteResults.put(label, data);
                if (!label.equals("TOTALT")){
                    partiesInVoteList.add(label);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        }

    }

    public HashMap<String, int[]> getVoteResults() {
        return voteResults;
    }

    public int[] getPartyVotes(String party){
        return voteResults.get(party);
    }

    public int[] getTotal(){
        return voteResults.get("TOTALT");
    }

    public ArrayList<String> getPartiesInVote(){
        return partiesInVoteList;
    }

}
